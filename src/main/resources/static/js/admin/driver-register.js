function getCookie(name) {
    var match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

var token = getCookie("token");
var userId = getCookie("userId");
var role = getCookie("role");

if (!token || role !== "ADMIN") {
    window.location.href = "/auth/login";
}

async function carregarDados() {
    try {
        var [driversRes, userRes] = await Promise.all([
            fetch('/api/admin/' + userId + '/drivers', {
                headers: { 'Authorization': 'Bearer ' + token }
            }),
            fetch('/user/' + userId, {
                headers: { 'Authorization': 'Bearer ' + token }
            })
        ]);

        if (!driversRes.ok) {
            if (driversRes.status === 403) { window.location.href = '/auth/login'; return; }
            throw new Error('Erro ' + driversRes.status);
        }

        var drivers = await driversRes.json();
        renderizarMotoristas(drivers);

        if (userRes.ok) {
            var userData = await userRes.json();
            atualizarAvatar(userData);
        }
    } catch (err) {
        document.getElementById('loadingView').innerHTML = '<div class="text-center"><p class="text-danger">Erro ao carregar dados: ' + err.message + '</p><button class="btn btn-submit mt-2" onclick="location.reload()">Tentar novamente</button></div>';
    }
}

function atualizarAvatar(userData) {
    var avatar = document.getElementById('adminAvatar');
    if (userData.userImageUrl && userData.userImageUrl !== 'padrao') {
        avatar.style.background = 'url(' + userData.userImageUrl + ') center/cover no-repeat';
        avatar.textContent = '';
    } else {
        var name = userData.fullName || 'Admin';
        var parts = name.trim().split(/\s+/);
        var initials = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0].substring(0, 2);
        avatar.textContent = initials.toUpperCase();
    }
}

function renderizarMotoristas(drivers) {
    document.getElementById('loadingView').style.display = 'none';
    document.getElementById('mainContent').style.display = 'block';

    var container = document.getElementById('driverList');
    container.innerHTML = '';

    if (drivers.length === 0) {
        container.innerHTML = '<div class="empty-state"><i class="bi bi-people"></i><p>Nenhum motorista cadastrado ainda.</p></div>';
        return;
    }

    drivers.forEach(function(d) {
        var card = document.createElement('div');
        card.className = 'driver-card shadow-sm';

        var name = d.fullName || 'Desconhecido';
        var parts = name.trim().split(/\s+/);
        var initials = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0].substring(0, 2);

        card.innerHTML =
            '<div class="driver-avatar-small">' + initials.toUpperCase() + '</div>' +
            '<div class="driver-info">' +
                '<div class="driver-name">' + escapeHtml(name) + '</div>' +
                '<div class="driver-details">' + escapeHtml(d.email || '') + '</div>' +
                '<div class="driver-licence"><i class="bi bi-card-text me-1"></i>CNH: ' + escapeHtml(d.licence || '---') + '</div>' +
            '</div>';

        container.appendChild(card);
    });
}

document.getElementById("driverForm").addEventListener("submit", async function(e) {
    e.preventDefault();

    var btn = document.getElementById("btnSubmit");
    var originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>CADASTRANDO...';

    var data = {
        fullName: document.getElementById("driverName").value,
        email: document.getElementById("driverEmail").value,
        password: document.getElementById("driverPassword").value,
        licence: document.getElementById("driverLicence").value,
        adress: document.getElementById("driverAdress").value,
        city: document.getElementById("driverCity").value
    };

    try {
        var res = await fetch('/api/admin/' + userId + '/drivers', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify(data)
        });

        var resultDiv = document.getElementById('formResult');

        if (res.ok) {
            resultDiv.innerHTML = '<div class="result-box success">Motorista cadastrado com sucesso!</div>';
            document.getElementById('driverForm').reset();
            setTimeout(function() { resultDiv.innerHTML = ''; }, 3000);
            var driversRes = await fetch('/api/admin/' + userId + '/drivers', {
                headers: { 'Authorization': 'Bearer ' + token }
            });
            if (driversRes.ok) {
                renderizarMotoristas(await driversRes.json());
            }
        } else {
            var err = await res.text();
            resultDiv.innerHTML = '<div class="result-box error">Erro: ' + escapeHtml(err) + '</div>';
        }
    } catch (err) {
        document.getElementById('formResult').innerHTML = '<div class="result-box error">Erro de rede</div>';
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
});

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(String(text)));
    return div.innerHTML;
}

function editarPerfil() {
    carregarDadosPerfil();
    var offcanvas = new bootstrap.Offcanvas(document.getElementById('offcanvasEditarPerfil'));
    offcanvas.show();
}

async function carregarDadosPerfil() {
    try {
        var res = await fetch('/user/' + userId, {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!res.ok) return;
        var data = await res.json();
        document.getElementById('editFullName').value = data.fullName || '';
        document.getElementById('editAdress').value = data.adress || '';
        document.getElementById('editCity').value = data.city || '';
        if (data.birthday) document.getElementById('editBirthDate').value = data.birthday;
        var preview = document.getElementById('editImagePreview');
        if (data.userImageUrl && data.userImageUrl !== 'padrao') {
            preview.src = data.userImageUrl;
            preview.style.display = 'block';
        } else {
            preview.src = '';
            preview.style.display = 'none';
        }
        if (data.userImageUrl) document.getElementById('editUserImageUrl').value = data.userImageUrl;
    } catch (e) { console.error('Erro ao carregar perfil:', e); }
}

document.getElementById('editImageInput').addEventListener('change', function() {
    var file = this.files[0];
    if (!file) return;
    var preview = document.getElementById('editImagePreview');
    preview.src = URL.createObjectURL(file);
    preview.style.display = 'block';
    preview.style.width = '100px';
    preview.style.height = '100px';
    preview.style.borderRadius = '50%';
    preview.style.objectFit = 'cover';
    preview.style.border = '3px solid var(--accent-orange)';
    preview.style.margin = '0 auto';
});

document.getElementById('btnSalvarPerfil').addEventListener('click', async function() {
    var btn = this;
    var originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Salvando...';
    try {
        var imageUrl = document.getElementById('editUserImageUrl').value || 'padrao';
        var fileInput = document.getElementById('editImageInput');
        if (fileInput.files.length > 0) {
            var formData = new FormData();
            formData.append('file', fileInput.files[0]);
            var uploadRes = await fetch('/api/upload/image', {
                method: 'POST',
                headers: { 'Authorization': 'Bearer ' + token },
                body: formData
            });
            if (uploadRes.ok) {
                var uploadData = await uploadRes.json();
                imageUrl = uploadData.imageUrl;
            }
        }
        var params = new URLSearchParams();
        params.append('fullName', document.getElementById('editFullName').value);
        params.append('adress', document.getElementById('editAdress').value);
        params.append('city', document.getElementById('editCity').value);
        params.append('birthDate', document.getElementById('editBirthDate').value);
        params.append('userImageUrl', imageUrl);
        var updateRes = await fetch('/user/' + userId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Authorization': 'Bearer ' + token },
            body: params.toString()
        });
        if (updateRes.ok) {
            var offcanvas = bootstrap.Offcanvas.getInstance(document.getElementById('offcanvasEditarPerfil'));
            if (offcanvas) offcanvas.hide();
            location.reload();
        } else { alert('Erro ao atualizar perfil.'); }
    } catch (e) { console.error('Erro:', e); alert('Erro de conexão.'); }
    finally { btn.disabled = false; btn.innerHTML = originalText; }
});

function sair() {
    document.cookie = 'token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'userId=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'driverId=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'role=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    window.location.href = '/auth/login';
}

carregarDados();
