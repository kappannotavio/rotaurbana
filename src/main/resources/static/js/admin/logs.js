function getCookie(name) {
    var match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

var token = getCookie("token");
var userId = getCookie("userId");
var role = getCookie("role");
var allLogs = [];

if (!token || role !== "ADMIN") {
    window.location.href = "/auth/login";
}

document.addEventListener("DOMContentLoaded", function() {
    carregarLogs();
});

async function carregarLogs() {
    try {
        var [userRes, logsRes] = await Promise.all([
            fetch("/user/" + userId, { headers: { 'Authorization': 'Bearer ' + token } }),
            fetch("/api/admin/" + userId + "/logs", { headers: { 'Authorization': 'Bearer ' + token } })
        ]);

        if (!logsRes.ok) { mostrarErro("Erro ao carregar logs"); return; }

        var userData = await userRes.json();
        allLogs = await logsRes.json();

        atualizarAvatar(userData);
        renderizarLogs(allLogs);

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

function renderizarLogs(logs) {
    var container = document.getElementById("logList");
    container.innerHTML = '';

    if (!logs || logs.length === 0) {
        container.innerHTML = '<div class="text-muted-custom text-center py-5">Nenhum registro encontrado</div>';
        return;
    }

    logs.forEach(function(log) {
        var item = document.createElement("div");
        item.className = "log-entry";

        var actionColor = getActionColor(log.action, log.entityType);

        var timestamp = '';
        if (log.timestamp) {
            var d = new Date(log.timestamp);
            timestamp = d.toLocaleString('pt-BR', {
                day: '2-digit', month: '2-digit', year: 'numeric',
                hour: '2-digit', minute: '2-digit'
            });
        }

        item.innerHTML =
            '<div class="log-indicator" style="background-color:' + actionColor + ';"></div>' +
            '<div class="log-content">' +
                '<div class="log-header">' +
                    '<span class="log-badge" style="background-color:' + actionColor + '20;color:' + actionColor + ';border:1px solid ' + actionColor + '40;">' +
                        escapeHtml(log.action) +
                    '</span>' +
                    '<span class="log-entity badge-entity" data-entity="' + escapeHtml(log.entityType || '') + '">' +
                        escapeHtml(log.entityType || '') +
                    '</span>' +
                    '<span class="log-time">' + timestamp + '</span>' +
                '</div>' +
                '<div class="log-desc">' + escapeHtml(log.description) + '</div>' +
                '<div class="log-by" style="font-size:0.7rem;color:var(--text-inactive);">' +
                    (log.performedByName ? 'Por: ' + escapeHtml(log.performedByName) : '') +
                '</div>' +
            '</div>';

        container.appendChild(item);
    });
}

function getActionColor(action, entityType) {
    if (!action) return 'var(--text-muted)';
    var a = action.toUpperCase();
    if (a === 'CRIADO') return 'var(--accent-green)';
    if (a === 'CONFIRMOU') return 'var(--accent-orange)';
    if (a === 'INSCREVEU') return '#0088ff';
    if (a === 'DESINSCREVEU' || a === 'REMOVIDO') return 'var(--accent-red)';
    if (a === 'ATUALIZADO' || a === 'ATUALIZOU') return '#a78bfa';
    return 'var(--text-muted)';
}

async function filtrarTipo(btn, tipo) {
    document.querySelectorAll('.filter-btn').forEach(function(b) { b.classList.remove('active'); });
    btn.classList.add('active');

    try {
        var url = "/api/admin/" + userId + "/logs";
        if (tipo) url += "?type=" + tipo;

        var res = await fetch(url, { headers: { 'Authorization': 'Bearer ' + token } });
        if (!res.ok) return;
        var logs = await res.json();
        renderizarLogs(logs);
    } catch (err) {}
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
