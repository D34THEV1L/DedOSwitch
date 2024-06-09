document.addEventListener("DOMContentLoaded", () => {
    const wsUrl = 'wss://free.blr2.piesocket.com/v3/1?api_key=sjBvbMQvr2A15szaGTBfhNbYCXlKO4kXagvTT0rg&notify_self=1';
    let ws = new WebSocket(wsUrl);
    let autoVerificationInterval = null;
    let scriptExecutionTimeout = null;
    let currentScript = null;
    const toasts = document.querySelector('.toasts');
    const checkbox = document.querySelector('.checkbox');
    const logContainer = document.getElementById('log');
    const scriptStatus = document.getElementById('scriptStatus');
    const runScriptBtn = document.getElementById('runScriptBtn');
    const stopScriptBtn = document.getElementById('stopScriptBtn');
    let userList = loadUserListFromStorage();
    let isScriptRunning = false;

    ws.onopen = () => {
        console.log('WebSocket connection opened');
        showToast("WebSocket conectado", "info");
        renderUserList();
        userList.forEach(user => {
            const message = JSON.stringify({ type: 'verifyuser', usuario: user.username });
            ws.send(message);
            user.status = 'unknown';
            renderUserList();
            logMessage('Sent', message);
        });
        showToast("Usuarios Verificados", "info");
    };

    ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        logMessage('Received', message);
        handleWebSocketMessage(message);
    };

    ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        showToast("WebSocket error", "error");
    };

    ws.onclose = () => {
        console.log('WebSocket connection closed');
        showToast("WebSocket cerrado, intentando reconectar...", "error");
        setTimeout(() => {
            ws = new WebSocket(wsUrl);
        }, 5000); // Intentar reconectar después de 5 segundos
    };

    document.getElementById('addUserBtn').addEventListener('click', () => {
        document.getElementById('addUserModal').style.display = 'block';
    });

    document.getElementById('confirmAddUserBtn').addEventListener('click', () => {
        const username = document.getElementById('usernameInput').value;
        if (username && isValidUsername(username)) {
            if (!isUserInList(username)) {
                const message = JSON.stringify({ type: 'checkuser', usuario: username });
                ws.send(message);
                logMessage('Sent', message);
                showToast("Añadiendo usuario", "info");
                document.getElementById('addUserModal').style.display = 'none';
            } else {
                showToast("Usuario ya existe", "error");
            }
        } else {
            showToast("Nombre de usuario inválido", "error");
        }
    });

    document.querySelector('.close-button').addEventListener('click', () => {
        document.getElementById('addUserModal').style.display = 'none';
    });

    document.getElementById('checkUsersBtn').addEventListener('click', () => {
        userList.forEach(user => {
            const message = JSON.stringify({ type: 'verifyuser', usuario: user.username });
            ws.send(message);
            user.status = 'unknown';
            renderUserList();
            logMessage('Sent', message);
        });
        showToast("Usuarios Verificados", "info");
    });



    checkbox.addEventListener('change', (event) => {
        if (event.target.checked) {
            showToast("Auto Verificación activada", "info");
            autoVerificationInterval = setInterval(() => {
                userList.forEach(user => {
                    const message = JSON.stringify({ type: 'verifyuser', usuario: user.username });
                    ws.send(message);
                    logMessage('Sent', message);
                });
                showToast("Verificando Usuarios", "info");
            }, 300000); // 5 minutos
        } else {
            showToast("Auto Verificación desactivada", "info");
            clearInterval(autoVerificationInterval);
        }
    });

    document.getElementById('sendCommandBtn').addEventListener('click', () => {
        const commandInput = document.getElementById('commandInput').value;
        if (commandInput) {
            const message = JSON.stringify({ type: 'command', command: commandInput });
            ws.send(message);
            logMessage('Sent', message);
            showToast("Comando enviado", "info");
            document.getElementById('commandInput').value = '';
        }
    });

    document.getElementById('clearLogBtn').addEventListener('click', () => {
        document.getElementById('log').innerHTML = '';
    });

    document.getElementById('clearConsoleBtn').addEventListener('click', () => {
        document.getElementById('scriptConsole').innerHTML = '';
    });

    let editor = CodeMirror.fromTextArea(document.getElementById('scriptEditor'), {
        mode: "javascript",
        lineNumbers: true
    });

    runScriptBtn.addEventListener('click', () => {
        const scriptContent = editor.getValue();
        if (scriptContent) {
            runScript(scriptContent);
        }
    });

    stopScriptBtn.addEventListener('click', () => {
        stopScript();
    });

    function runScript(scriptContent) {
        try {
            currentScript = new Function('stop', 'api', scriptContent);
            currentScript(stopScript, window.api);
            scriptExecutionTimeout = setTimeout(() => {
                stopScript();
                showToast("Script detenido por tiempo máximo", "error");
            }, 30000); // Detener script después de 30 segundos
            scriptStatus.textContent = "Estado: Corriendo";
            showToast("Script ejecutado", "info");
        } catch (error) {
            handleScriptError(error);
        }
    }

    function stopScript() {
        if (scriptExecutionTimeout) {
            clearTimeout(scriptExecutionTimeout);
        }
        if (currentScript) {
            try {
                currentScript = null;
                scriptStatus.textContent = "Estado: Detenido";
                showToast("Script detenido", "info");
            } catch (error) {
                handleScriptError(error);
            }
        }
    }

    function handleScriptError(error) {
        console.error('Script error:', error);
        document.getElementById('scriptConsole').textContent += `Error: ${error.message}\n`;
        showToast("Error al ejecutar script", "error");
    }

    function logMessage(direction, message) {
        const log = document.getElementById('log');
        const logEntry = document.createElement('div');
        logEntry.className = 'log-entry';
        logEntry.textContent = `${direction}: ${JSON.stringify(message)}`;
        log.appendChild(logEntry);
    }

    function showToast(message, type) {
        const toast = document.createElement('div');
        toast.className = `toast toast--${type}`;
        toast.textContent = message;
        toasts.appendChild(toast);
        setTimeout(() => {
            toast.remove();
        }, 5000);
    }

    function handleWebSocketMessage(message) {
        if (message.type === 'responseuser') {
            const user = {
                userid: message.userid,
                username: message.usuario,
                status: message.status,
                farmServer: '',
                dropServer: '',
                emulatorid: message.emulatorid
            };
            addUser(user);
        } else if (message.type === 'verifyuserresponse') {
            updateUserStatus(message);
        } else if (message.type === 'error') {
            showToast(message.error, "error");
        } else {
            console.log('Unhandled WebSocket message type:', message.type);
        }
    }

    function isUserInList(username) {
        return userList.some(user => user.username === username);
    }

    function addUser(user) {
        if (!isUserInList(user.username)) {
            user.status = 'unknown';
            user.lastResponse = 'N/A';
            user.imageUrl = '';
            userList.push(user);
            saveUserListToStorage();
            if (user.userid !== 'undefined') {
                getProfileImage(user.userid, (imageUrl) => {
                    updateUserImage(user.username, imageUrl);
                    renderUserList();
                });
            }
            renderUserList();
        } else {
            showToast("Usuario inválido o ya existe", "error");
        }
    }

    function updateUserStatus(data) {
        const user = userList.find(user => user.username === data.usuario);
        if (user) {
            user.status = data.status ? 'true' : 'false';
            user.lastResponse = new Date().toLocaleString();
            saveUserListToStorage();
            renderUserList();
        }
    }

    function updateUserImage(username, imageUrl) {
        const user = userList.find(user => user.username === username);
        if (user) {
            user.imageUrl = imageUrl;
            saveUserListToStorage();
            renderUserList();
        }
    }

    
    function deleteUser(username) {
        // Verifica si el username es undefined
        if (username === 'undefined' || username === undefined || username === null) {
            // Borra toda la lista de usuarios
            userList = [];
            saveUserListToStorage();
            showToast("Se detectó un usuario inválido. Lista de usuarios eliminada.", "error");
            logMessage('Log', { action: "Usuario inválido detectado, lista de usuarios eliminada" });
        } else {
            // Filtra la lista de usuarios para eliminar el usuario con el username dado
            userList = userList.filter(user => user.username !== username);
            saveUserListToStorage();
            showToast(`Usuario ${username} eliminado.`, "success");
            logMessage('Log', { action: `Usuario ${username} eliminado` });
        }
    
        // Vuelve a renderizar la lista de usuarios
        renderUserList();
    }
    // Llama a renderUserList después de cargar la lista de usuarios desde el almacenamiento
    renderUserList();

function renderUserList() {
    const userContainer = document.getElementById('userContainer');
    userContainer.innerHTML = '';
    userList.forEach(user => {
        const userDiv = document.createElement('div');
        userDiv.className = 'user-info';
        userDiv.innerHTML = `
            <details>
                <summary>
                    <div class="user-summary">
                        <div class="user-photo">
                            <img src="${user.imageUrl || 'path/to/your/default/photo.jpg'}" alt="User Photo">
                        </div>
                        <div class="user-details">
                            <span>Usuario:</span> ${user.username}</span>
                            <span>ID:</span> ${user.userid}</span>
                            <div class="status">Estado: <span class="${user.status === 'true' ? 'text-success' : user.status === 'false' ? 'text-danger' : 'text-warning'}">${user.status}</span></div>
                            <div class="last-response">Última Respuesta: <span>${user.lastResponse}</span></div>
                            <span>EmulatorId:</span> ${user.emulatorid}</span>
                        </div>
                        <button class="delete-user-btn" data-username="${user.username}">Eliminar</button>
                    </div>
                </summary>
                <div class="user-extra">
                    <label>Server Farm: <input type="text" class="farm-server-input" value="${user.farmServer}"></label>
                    <label>Server Drop: <input type="text" class="drop-server-input" value="${user.dropServer}"></label>
                    <button class="join-farm-btn">Join to Farm Server</button>
                    <button class="join-drop-btn">Join to Drop Server</button>
                </div>
            </details>
        `;
        userContainer.appendChild(userDiv);
    });

    const deleteButtons = document.querySelectorAll('.delete-user-btn');
    deleteButtons.forEach(button => {
        button.addEventListener('click', () => {
            const username = button.getAttribute('data-username');
            deleteUser(username);
        });
    });

    const inputs = document.querySelectorAll('.farm-server-input, .drop-server-input');
    inputs.forEach(input => {
        input.addEventListener('change', (event) => {
            const username = event.target.closest('.user-info').querySelector('.delete-user-btn').getAttribute('data-username');
            const user = userList.find(user => user.username === username);
            if (user) {
                if (event.target.classList.contains('farm-server-input')) {
                    user.farmServer = event.target.value;
                } else if (event.target.classList.contains('drop-server-input')) {
                    user.dropServer = event.target.value;
                }
                saveUserListToStorage();
            }
        });
    });

    const joinFarmButtons = document.querySelectorAll('.join-farm-btn');
    joinFarmButtons.forEach(button => {
        button.addEventListener('click', (event) => {
            const username = event.target.closest('.user-info').querySelector('.delete-user-btn').getAttribute('data-username');
            const user = userList.find(user => user.username === username);
            if (user && user.farmServer) {
                const message = JSON.stringify({ type: 'joinfarm', usuario: user.username, farmServer: user.farmServer });
                ws.send(message);
                logMessage('Sent', message);
                showToast(`Joined ${user.farmServer}`, "info");
            } else {
                showToast("Server Farm no puede estar vacío", "error");
            }
        });
    });

    const joinDropButtons = document.querySelectorAll('.join-drop-btn');
    joinDropButtons.forEach(button => {
        button.addEventListener('click', (event) => {
            const username = event.target.closest('.user-info').querySelector('.delete-user-btn').getAttribute('data-username');
            const user = userList.find(user => user.username === username);
            if (user && user.dropServer) {
                const message = JSON.stringify({ type: 'joindrop', usuario: user.username, dropServer: user.dropServer });
                ws.send(message);
                logMessage('Sent', message);
                showToast(`Joined ${user.dropServer}`, "info");
            } else {
                showToast("Server Drop no puede estar vacío", "error");
            }
        });
    });
}

    function getProfileImage(userId, callback) {
        const url = `https://thumbnails.roblox.com/v1/users/avatar?userIds=${userId}&size=420x420&format=Png&isCircular=false`;

        fetch(url, {
            method: 'GET',
            headers: {
                'accept': 'application/json'
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data && data.data && data.data.length > 0 && data.data[0].state === "Completed") {
                const imageUrl = data.data[0].imageUrl;
                callback(imageUrl);
            } else {
                console.error('Error al obtener la imagen de perfil:', data);
                showToast("Error al obtener la imagen de perfil", "error");
                callback('default-user.png'); // URL de imagen predeterminada si no se puede obtener la imagen real
            }
        })
        .catch(error => {
            console.error('Error en la solicitud de imagen de perfil:', error);
            showToast("Error en la solicitud de imagen de perfil", "error");
            callback('default-user.png'); // URL de imagen predeterminada si ocurre un error
        });
    }

    function saveUserListToStorage() {
        localStorage.setItem('userList', JSON.stringify(userList));
    }

    function loadUserListFromStorage() {
        const storedList = localStorage.getItem('userList');
        return storedList ? JSON.parse(storedList) : [];
    }

    function isValidUsername(username) {
        const usernamePattern = /^[a-zA-Z0-9_]+$/;
        return usernamePattern.test(username);
    }

    function apiCall(endpoint, method = 'GET', body = null) {
        const url = `https://api.example.com/${endpoint}`;
        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            }
        };
        if (body) {
            options.body = JSON.stringify(body);
        }
        return fetch(url, options)
            .then(response => response.json())
            .catch(error => {
                console.error('API error:', error);
                throw error;
            });
    }

    window.api = {
        call: apiCall,
        addUser: addUser,
        deleteUser: deleteUser,
        updateUserStatus: updateUserStatus,
        updateUserImage: updateUserImage
    };

    window.scriptAPI = {
        sendMessage: (message) => {
            ws.send(JSON.stringify(message));
        },
        showToast: showToast,
        logToConsole: (message) => {
            const logEntry = document.createElement('div');
            logEntry.className = 'script-log-entry';
            logEntry.innerHTML = `<strong>Script Log:</strong> ${typeof message === 'string' ? message : JSON.stringify(message)}`;
            document.getElementById('scriptConsole').appendChild(logEntry);
        },
        logToUI: (message) => {
            const logEntry = document.createElement('div');
            logEntry.className = 'log-entry';
            logEntry.innerHTML = `<strong>Log:</strong> ${typeof message === 'string' ? message : JSON.stringify(message)}`;
            document.getElementById('log').appendChild(logEntry);
        },
        addButtonToUser: (username, buttonLabel, callback) => {
            const userDiv = [...document.getElementsByClassName('user-info')].find(div => {
                const details = div.getElementsByClassName('user-details')[0];
                return details && details.textContent.includes(username);
            });
            if (userDiv) {
                const userExtraDiv = userDiv.querySelector('.user-extra');
                if (userExtraDiv) {
                    const button = document.createElement('button');
                    button.classList.add('button--apicreated')
                    button.textContent = buttonLabel;
                    button.addEventListener('click', callback);
                    userExtraDiv.appendChild(button);
                } else {
                    showToast(`Clase 'user-extra' no encontrada dentro de 'user-info'`, "error");
                }
            } else {
                showToast(`Usuario ${username} no encontrado`, "error");
            }
        },
        
        removeUser: (username) => {
            const userDiv = [...document.getElementsByClassName('user-info')].find(div => {
                const details = div.getElementsByClassName('user-details')[0];
                return details && details.textContent.includes(username);
            });
            if (userDiv) {
                userDiv.remove();
                showToast(`Usuario ${username} eliminado`, "info");
            } else {
                showToast(`Usuario ${username} no encontrado`, "error");
            }
        },
        getUserInfo: (username) => {
            const userDiv = [...document.getElementsByClassName('user-info')].find(div => {
                const details = div.getElementsByClassName('user-details')[0];
                return details && details.textContent.includes(username);
            });
            if (userDiv) {
                return {
                    username: userDiv.querySelector('.user-details span').textContent,
                    status: userDiv.querySelector('.status span').textContent,
                    lastResponse: userDiv.querySelector('.last-response span').textContent
                };
            } else {
                showToast(`Usuario ${username} no encontrado`, "error");
                return null;
            }
        }
    };


});
