function getCookie(name) {
    var match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

var token = getCookie("token");
var userId = getCookie("userId");
var role = getCookie("role");
var allUsers = [];

if (!token || role !== "ADMIN") {
    window.location.href = "/auth/login";
}

document.addEventListener("DOMContentLoaded", function() {
    carregarDados();
    setupEventos();
});

function setupEventos() {
    document.getElementById("editUserForm").addEventListener("submit", async function(e) {
        e.preventDefault();
        salvarUsuario();
    });
}

async function carregarDados() {
    try {
        var [userRes, usersRes] = await Promise.all([
            fetch("/user/" + userId, { headers: { 'Authorization': 'Bearer ' + token } }),
            fetch("/api/admin/" + userId + "/users", { headers: { 'Authorization': 'Bearer ' + token } })
        ]);

        if (!usersRes.ok) { mostrarErro("Erro ao carregar usuários"); return; }

        var userData = await userRes.json();
        allUsers = await usersRes.json();

        atualizarAvatar(userData);
        atualizarStats(allUsers);
        renderizarUsuarios(allUsers);

        document.getElementById('loadingView').style.display = 'none';
        document.getElementById('mainContent').style.display = 'block';
    } catch (err) {
        mostrarErro("Erro de conexão");
    }
}

function atualizarAvatar(userData) {
    var avatar = document.getElementById("adminAvatar");
    if (userData.userImageUrl && userData.userImageUrl !== "padrao") {
        avatar.innerHTML = '<img src="' + userData.userImageUrl + '" style="width:100%;height:100%;border-radius:8px;object-fit:cover;">';
    } else {
        var initials = userData.fullName ? userData.fullName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : 'AD';
        avatar.textContent = initials;
    }
}

function atualizarStats(users) {
    var total = users.length;
    var drivers = users.filter(u => u.role === 'DRIVER').length;
    var passengers = users.filter(u => u.role === 'USER').length;
    document.getElementById("statTotalUsers").textContent = total;
    document.getElementById("statDrivers").textContent = drivers;
    document.getElementById("statPassengers").textContent = passengers;
}

function renderizarUsuarios(users) {
    var container = document.getElementById("userList");
    container.innerHTML = '';

    if (!users || users.length === 0) {
        container.innerHTML = '<div class="text-muted-custom text-center py-5">Nenhum usuário encontrado</div>';
        return;
    }

    users.forEach(function(u) {
        var card = document.createElement("div");
        card.className = "user-card shadow-sm";
        card.onclick = function() { abrirDetalhes(u.id); };

        var initials = u.fullName ? u.fullName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : '??';
        var avatarHtml = (u.userImageUrl && u.userImageUrl !== "padrao")
            ? '<img src="' + u.userImageUrl + '" class="user-card-avatar-img" alt="">'
            : '<div class="user-card-avatar-initials">' + initials + '</div>';

        var roleBadge = '';
        if (u.role === 'ADMIN') roleBadge = '<span class="badge-role-admin">ADMIN</span>';
        else if (u.role === 'DRIVER') roleBadge = '<span class="badge-role-driver">MOTORISTA</span>';
        else roleBadge = '<span class="badge-role-user">USUÁRIO</span>';

        var paymentBadge = '';
        if (u.paymentStatus === 'EM_DAY') paymentBadge = '<span class="badge-pay-ok"><i class="bi bi-check-circle"></i> Em dia</span>';
        else if (u.paymentStatus === 'PENDING') paymentBadge = '<span class="badge-pay-pending"><i class="bi bi-clock"></i> Pendente</span>';
        else if (u.paymentStatus === 'LATE') paymentBadge = '<span class="badge-pay-late"><i class="bi bi-exclamation-circle"></i> Atrasado</span>';

        card.innerHTML =
            '<div class="user-card-avatar">' + avatarHtml + '</div>' +
            '<div class="user-card-info">' +
                '<div class="user-card-name">' + escapeHtml(u.fullName) + '</div>' +
                '<div class="user-card-email">' + escapeHtml(u.email) + '</div>' +
                '<div class="user-card-meta">' +
                    roleBadge +
                    paymentBadge +
                '</div>' +
            '</div>' +
            '<div class="user-card-routes">' +
                '<span class="route-count">' + (u.routeCount || 0) + '</span>' +
                '<span class="route-label">rotas</span>' +
            '</div>' +
            '<div class="user-card-arrow"><i class="bi bi-chevron-right"></i></div>';

        container.appendChild(card);
    });
}

function buscarUsuarios() {
    var query = document.getElementById("searchInput").value.trim();
    if (query.length < 2) {
        renderizarUsuarios(allUsers);
        atualizarStats(allUsers);
        return;
    }

    fetch("/api/admin/" + userId + "/users/search?q=" + encodeURIComponent(query), {
        headers: { 'Authorization': 'Bearer ' + token }
    })
    .then(function(r) { return r.json(); })
    .then(function(users) {
        renderizarUsuarios(users);
        atualizarStats(users);
    })
    .catch(function() {});
}

async function abrirDetalhes(userIdDetalhes) {
    try {
        var res = await fetch("/api/admin/" + userId + "/users/" + userIdDetalhes, {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!res.ok) { alert("Erro ao carregar detalhes"); return; }

        var data = await res.json();

        document.getElementById("modalUserTitle").textContent = "Detalhes do Usuário";
        document.getElementById("userDetailName").textContent = data.fullName;
        document.getElementById("userDetailRole").textContent = data.role;

        var initials = data.fullName ? data.fullName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : '--';
        var avatarEl = document.getElementById("userDetailAvatar");
        if (data.userImageUrl && data.userImageUrl !== "padrao") {
            avatarEl.innerHTML = '<img src="' + data.userImageUrl + '" style="width:64px;height:64px;border-radius:50%;object-fit:cover;">';
        } else {
            avatarEl.textContent = initials;
            avatarEl.style.display = 'flex';
        }

        var paymentBadge = document.getElementById("userDetailPayment");
        if (data.paymentStatus === 'EM_DAY') paymentBadge.textContent = 'Em dia';
        else if (data.paymentStatus === 'PENDING') paymentBadge.textContent = 'Pendente';
        else if (data.paymentStatus === 'LATE') paymentBadge.textContent = 'Atrasado';

        document.getElementById("editUserFullName").value = data.fullName || '';
        document.getElementById("editUserEmail").value = data.email || '';
        document.getElementById("editUserAdress").value = data.adress || '';
        document.getElementById("editUserCity").value = data.city || '';
        document.getElementById("editUserPaymentStatus").value = data.paymentStatus || 'EM_DAY';
        document.getElementById("editUserResult").innerHTML = '';

        document.getElementById("editUserForm").dataset.userId = userIdDetalhes;

        renderizarRotasUsuario(data.routes || []);

        var modalEl = document.getElementById('modalUserDetails');
        var modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        modal.show();
    } catch (err) {
        alert("Erro de conexão");
    }
}

function renderizarRotasUsuario(routes) {
    var container = document.getElementById("userRoutesList");
    container.innerHTML = '';

    if (!routes || routes.length === 0) {
        container.innerHTML = '<div class="text-muted-custom text-center py-3">Nenhuma rota inscrita</div>';
        return;
    }

    routes.forEach(function(r) {
        var item = document.createElement("div");
        item.className = "route-item d-flex justify-content-between align-items-center p-3";
        item.style.backgroundColor = 'var(--bg-card-light)';
        item.style.borderRadius = '8px';
        item.innerHTML =
            '<div>' +
                '<div class="fw-bold" style="font-size:0.9rem;">' + escapeHtml(r.departurePoint || 'Origem') + ' → ' + escapeHtml(r.destiny) + '</div>' +
                '<div style="font-size:0.75rem;color:var(--text-muted);">' +
                    (r.departureTime || '--') + ' • ' + (r.busSign ? 'Ônibus ' + r.busSign : '') +
                '</div>' +
            '</div>' +
            '<button class="btn btn-sm" style="background-color:rgba(247,90,104,0.15);color:var(--accent-red);border:none;border-radius:6px;padding:6px 12px;" onclick="removerDaRota(' + r.idRoute + ', event)">' +
                '<i class="bi bi-x-circle"></i> Remover' +
            '</button>';
        container.appendChild(item);
    });
}

async function removerDaRota(routeId, event) {
    event.stopPropagation();
    if (!confirm("Tem certeza que deseja remover este usuário da rota?")) return;

    var userIdDetalhes = document.getElementById("editUserForm").dataset.userId;

    try {
        var res = await fetch("/api/admin/" + userId + "/users/" + userIdDetalhes + "/routes/" + routeId, {
            method: 'DELETE',
            headers: { 'Authorization': 'Bearer ' + token }
        });

        if (res.ok) {
            abrirDetalhes(userIdDetalhes);
        } else {
            var err = await res.text();
            alert("Erro: " + err);
        }
    } catch (err) {
        alert("Erro de conexão");
    }
}

async function salvarUsuario() {
    var btn = document.getElementById("btnSaveUser");
    var originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>SALVANDO...';

    var userIdDetalhes = document.getElementById("editUserForm").dataset.userId;

    var data = {
        fullName: document.getElementById("editUserFullName").value,
        adress: document.getElementById("editUserAdress").value,
        city: document.getElementById("editUserCity").value,
        paymentStatus: document.getElementById("editUserPaymentStatus").value
    };

    try {
        var res = await fetch("/api/admin/" + userId + "/users/" + userIdDetalhes, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify(data)
        });

        var resultDiv = document.getElementById("editUserResult");
        if (res.ok) {
            resultDiv.innerHTML = '<div class="result-box success">Usuário atualizado com sucesso!</div>';
            setTimeout(function() { resultDiv.innerHTML = ''; }, 3000);
            carregarDados();
        } else {
            var err = await res.text();
            resultDiv.innerHTML = '<div class="result-box error">Erro: ' + err + '</div>';
        }
    } catch (err) {
        document.getElementById("editUserResult").innerHTML = '<div class="result-box error">Erro de rede</div>';
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
}

function editarPerfil() {
    var offcanvas = new bootstrap.Offcanvas(document.getElementById('offcanvasEditarPerfil'));
    offcanvas.show();
    carregarDadosPerfil();
}

async function carregarDadosPerfil() {
    try {
        var res = await fetch("/user/" + userId, { headers: { 'Authorization': 'Bearer ' + token } });
        if (!res.ok) return;
        var data = await res.json();
        document.getElementById("editFullName").value = data.fullName || '';
        document.getElementById("editAdress").value = data.adress || '';
        document.getElementById("editCity").value = data.city || '';
        document.getElementById("editBirthDate").value = data.birthDate || '';
        document.getElementById("editBirthDate").setAttribute("max", new Date().toISOString().split("T")[0]);
        document.getElementById("editUserImageUrl").value = data.userImageUrl || '';
        if (data.userImageUrl && data.userImageUrl !== "padrao") {
            var preview = document.getElementById("editImagePreview");
            preview.src = data.userImageUrl;
            preview.style.display = "block";
        }
    } catch (err) {}
}

document.getElementById("editImageInput").addEventListener("change", function() {
    var preview = document.getElementById("editImagePreview");
    preview.src = URL.createObjectURL(this.files[0]);
    preview.style.display = "block";
});

document.getElementById("btnSalvarPerfil").addEventListener("click", async function() {
    var btn = this;
    btn.disabled = true;
    btn.textContent = "Salvando...";

    try {
        var imageUrl = document.getElementById("editUserImageUrl").value || "padrao";
        var fileInput = document.getElementById("editImageInput");
        if (fileInput.files.length > 0) {
            var formData = new FormData();
            formData.append("file", fileInput.files[0]);
            var uploadRes = await fetch("/api/upload/image", {
                method: "POST",
                headers: { 'Authorization': 'Bearer ' + token },
                body: formData
            });
            if (uploadRes.ok) {
                var uploadData = await uploadRes.json();
                imageUrl = uploadData.imageUrl;
            }
        }

        var params = new URLSearchParams();
        params.append("fullName", document.getElementById("editFullName").value);
        params.append("adress", document.getElementById("editAdress").value);
        params.append("city", document.getElementById("editCity").value);
        params.append("birthDate", document.getElementById("editBirthDate").value);
        params.append("userImageUrl", imageUrl);

        var res = await fetch("/user/" + userId, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Authorization': 'Bearer ' + token
            },
            body: params.toString()
        });

        if (res.ok) {
            carregarDados();
            var offcanvas = bootstrap.Offcanvas.getInstance(document.getElementById('offcanvasEditarPerfil'));
            if (offcanvas) offcanvas.hide();
        }
    } catch (err) {}
    btn.disabled = false;
    btn.textContent = "Salvar Alterações";
});

function mostrarErro(msg) {
    document.getElementById('loadingView').innerHTML =
        '<div class="text-center">' +
            '<i class="bi bi-exclamation-triangle" style="font-size:3rem;color:var(--accent-red);"></i>' +
            '<p class="text-danger mt-3">' + msg + '</p>' +
            '<button class="btn btn-outline-light mt-2" onclick="location.reload()">Tentar novamente</button>' +
        '</div>';
}

function sair() {
    document.cookie = "token=; path=/; max-age=0";
    document.cookie = "userId=; path=/; max-age=0";
    document.cookie = "driverId=; path=/; max-age=0";
    document.cookie = "role=; path=/; max-age=0";
    window.location.href = "/auth/login";
}

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}
