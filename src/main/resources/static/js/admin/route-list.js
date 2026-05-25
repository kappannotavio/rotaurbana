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

var createMap = null, originMarker = null, destMarker = null;
var isSettingOrigin = true;

function makeIcon(color) {
    return L.divIcon({
        html: '<div style="background:' + color + ';width:20px;height:20px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.4);"></div>',
        iconSize: [20, 20], iconAnchor: [10, 10], className: ''
    });
}

async function carregarRotas() {
    try {
        var [routesRes, userRes] = await Promise.all([
            fetch('/api/admin/' + userId + '/routes', {
                headers: { 'Authorization': 'Bearer ' + token }
            }),
            fetch('/user/' + userId, {
                headers: { 'Authorization': 'Bearer ' + token }
            })
        ]);

        if (!routesRes.ok) {
            if (routesRes.status === 403) { window.location.href = '/auth/login'; return; }
            throw new Error('Erro ' + routesRes.status);
        }

        var routes = await routesRes.json();
        renderizarRotas(routes);

        if (userRes.ok) {
            var userData = await userRes.json();
            atualizarAvatar(userData);
        }
    } catch (err) {
        document.getElementById('loadingView').innerHTML = '<div class="text-center"><p class="text-danger">Erro ao carregar rotas: ' + err.message + '</p><button class="btn btn-nova-rota mt-2" onclick="location.reload()">Tentar novamente</button></div>';
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

function renderizarRotas(routes) {
    document.getElementById('loadingView').style.display = 'none';
    document.getElementById('mainContent').style.display = 'block';

    var activeRoutes = routes.filter(function(r) { return r.status === 'ativa'; });
    var totalPassengers = 0;
    var latePayments = 0;

    routes.forEach(function(r) {
        if (r.status === 'ativa') {
            totalPassengers += r.totalPassengers || 0;
            latePayments += (r.pendingCount || 0) + (r.lateCount || 0);
        }
    });

    document.getElementById('statActiveRoutes').textContent = activeRoutes.length;
    document.getElementById('statActiveDesc').textContent = 'de ' + routes.length + ' cadastradas';
    document.getElementById('statTotalPassengers').textContent = totalPassengers;
    document.getElementById('statLatePayments').textContent = latePayments;

    var container = document.getElementById('routeList');
    container.innerHTML = '';

    if (routes.length === 0) {
        container.innerHTML = '<div class="empty-state"><i class="bi bi-map"></i><p>Nenhuma rota cadastrada ainda.</p><button class="btn-nova-rota" onclick="abrirModalNovaRota()">+ CRIAR PRIMEIRA ROTA</button></div>';
        return;
    }

    routes.forEach(function(r) {
        var card = document.createElement('div');
        card.className = 'route-card status-' + (r.status || 'pendente') + ' shadow-sm';
        card.onclick = function() { window.location.href = '/admin/route-details/' + r.idRoute; };

        var origin = r.departurePoint || 'Origem';
        var dest = r.destiny || 'Destino';
        var badgeClass = r.status === 'ativa' ? 'ativa' : 'pendente';
        var badgeText = r.status === 'ativa' ? 'Ativa' : 'Pendente';
        var subtitle = '';
        if (r.busSign) subtitle += 'Ônibus ' + r.busSign;
        if (r.busBrand && r.busModel) subtitle += (subtitle ? ' • ' : '') + r.busBrand + ' ' + r.busModel;

        card.innerHTML =
            '<div class="d-flex justify-content-between align-items-start">' +
                '<div>' +
                    '<div class="route-title">' + escapeHtml(origin) + ' \u2192 ' + escapeHtml(dest) + '</div>' +
                    (subtitle ? '<div class="route-subtitle">' + escapeHtml(subtitle) + '</div>' : '<div class="route-subtitle">&nbsp;</div>') +
                '</div>' +
                '<div class="route-badge ' + badgeClass + '">' + badgeText + '</div>' +
            '</div>' +
            '<div class="route-stats-container">' +
                '<div class="route-stat-item">' +
                    '<div class="label">Passageiros</div>' +
                    '<div class="value">' + (r.totalPassengers || 0) + '</div>' +
                '</div>' +
                '<div class="route-stat-item">' +
                    '<div class="label">Pag. OK</div>' +
                    '<div class="value text-green">' + (r.emDayCount || 0) + '</div>' +
                '</div>' +
                '<div class="route-stat-item">' +
                    '<div class="label">Pag. Atras.</div>' +
                    '<div class="value text-red">' + ((r.pendingCount || 0) + (r.lateCount || 0)) + '</div>' +
                '</div>' +
                '<div class="route-stat-item">' +
                    '<div class="label">Horário</div>' +
                    '<div class="value">' + (r.departureTime || '-') + '</div>' +
                '</div>' +
                '<div class="route-stat-item">' +
                    '<div class="label">Motorista</div>' +
                    '<div class="value">' + escapeHtml(r.driverName || '-') + '</div>' +
                '</div>' +
            '</div>';
        container.appendChild(card);
    });
}

async function abrirModalNovaRota() {
    document.getElementById('routeForm').reset();
    document.getElementById('routeResult').innerHTML = '';

    var modal = new bootstrap.Modal(document.getElementById('modalNovaRota'));
    modal.show();

    document.getElementById('modalNovaRota').addEventListener('shown.bs.modal', function() {
        initCreateMap();
    }, { once: true });

    await carregarBusesCriacao();
}

async function carregarBusesCriacao() {
    var select = document.getElementById('busSelectRoute');
    select.innerHTML = '<option value="">Carregando ônibus...</option>';
    try {
        var res = await fetch('/api/admin/' + userId + '/buses', {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!res.ok) throw new Error();
        var buses = await res.json();
        select.innerHTML = '<option value="">Selecione um ônibus</option>';
        buses.forEach(function(b) {
            var opt = document.createElement('option');
            opt.value = b.idBus;
            opt.textContent = b.sign + ' - ' + (b.brand || '') + ' ' + (b.model || '') + ' (' + (b.driverName || 'Sem motorista') + ')';
            select.appendChild(opt);
        });
    } catch (e) {
        select.innerHTML = '<option value="">Erro ao carregar</option>';
    }
}

function initCreateMap() {
    var container = document.getElementById('createRouteMap');
    if (!container) return;

    if (createMap) {
        createMap.invalidateSize();
        return;
    }

    createMap = L.map('createRouteMap', { zoomControl: true }).setView([-29.6, -54.0], 13);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(createMap);

    createMap.on('click', function(e) {
        if (isSettingOrigin) {
            if (originMarker) createMap.removeLayer(originMarker);
            originMarker = L.circleMarker(e.latlng, {
                radius: 12, color: '#00b37e', fillColor: '#00b37e', fillOpacity: 0.9
            }).addTo(createMap).bindPopup('<b>Origem</b>').openPopup();
            document.getElementById('departureLatitude').value = e.latlng.lat.toFixed(6);
            document.getElementById('departureLongitude').value = e.latlng.lng.toFixed(6);
            isSettingOrigin = false;
        } else {
            if (destMarker) createMap.removeLayer(destMarker);
            destMarker = L.circleMarker(e.latlng, {
                radius: 12, color: '#ff5e00', fillColor: '#ff5e00', fillOpacity: 0.9
            }).addTo(createMap).bindPopup('<b>Destino</b>').openPopup();
            document.getElementById('destinationLatitude').value = e.latlng.lat.toFixed(6);
            document.getElementById('destinationLongitude').value = e.latlng.lng.toFixed(6);
            isSettingOrigin = true;
        }
    });
}

function geocode(query, callback) {
    fetch('https://nominatim.openstreetmap.org/search?format=json&limit=5&q=' + encodeURIComponent(query))
        .then(function(r) { return r.json(); })
        .then(function(data) { callback(data); })
        .catch(function() { alert('Erro ao buscar endereço'); });
}

document.getElementById('btnSearchOrigin').addEventListener('click', function() {
    var q = document.getElementById('searchOrigin').value.trim();
    if (!q) return;
    geocode(q, function(results) {
        if (!results || results.length === 0) { alert('Endereço não encontrado'); return; }
        var r = results[0];
        var lat = parseFloat(r.lat), lng = parseFloat(r.lon);
        if (originMarker) createMap.removeLayer(originMarker);
        originMarker = L.circleMarker([lat, lng], {
            radius: 12, color: '#00b37e', fillColor: '#00b37e', fillOpacity: 0.9
        }).addTo(createMap).bindPopup('<b>Origem</b><br>' + r.display_name).openPopup();
        document.getElementById('departureLatitude').value = lat.toFixed(6);
        document.getElementById('departureLongitude').value = lng.toFixed(6);
        document.getElementById('departureAddress').value = r.display_name;
        createMap.setView([lat, lng], 15);
        isSettingOrigin = false;
    });
});

document.getElementById('btnSearchDestiny').addEventListener('click', function() {
    var q = document.getElementById('searchDestiny').value.trim();
    if (!q) return;
    geocode(q, function(results) {
        if (!results || results.length === 0) { alert('Endereço não encontrado'); return; }
        var r = results[0];
        var lat = parseFloat(r.lat), lng = parseFloat(r.lon);
        if (destMarker) createMap.removeLayer(destMarker);
        destMarker = L.circleMarker([lat, lng], {
            radius: 12, color: '#ff5e00', fillColor: '#ff5e00', fillOpacity: 0.9
        }).addTo(createMap).bindPopup('<b>Destino</b><br>' + r.display_name).openPopup();
        document.getElementById('destinationLatitude').value = lat.toFixed(6);
        document.getElementById('destinationLongitude').value = lng.toFixed(6);
        document.getElementById('destinationAddress').value = r.display_name;
        createMap.setView([lat, lng], 15);
        isSettingOrigin = true;
    });
});

document.getElementById('btnRouteSubmit').addEventListener('click', async function() {
    var btn = this;
    var originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>CADASTRANDO...';

    var data = {
        busId: document.getElementById('busSelectRoute').value,
        destiny: document.getElementById('destiny').value,
        departurePoint: document.getElementById('departurePoint').value,
        departureAddress: document.getElementById('departureAddress').value,
        destinationAddress: document.getElementById('destinationAddress').value,
        estimatedDuration: document.getElementById('estimatedDuration').value,
        departureTime: document.getElementById('departureTime').value,
        destinationTime: document.getElementById('destinationTime').value,
        departureLatitude: document.getElementById('departureLatitude').value,
        departureLongitude: document.getElementById('departureLongitude').value,
        destinationLatitude: document.getElementById('destinationLatitude').value,
        destinationLongitude: document.getElementById('destinationLongitude').value
    };

    try {
        var res = await fetch('/api/admin/' + userId + '/routes', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify(data)
        });

        var resultDiv = document.getElementById('routeResult');

        if (res.ok) {
            resultDiv.innerHTML = '<div class="p-3 rounded" style="background:var(--accent-green-bg);border:1px solid var(--accent-green);color:var(--accent-green);font-weight:600;">Rota cadastrada com sucesso!</div>';
            document.getElementById('routeForm').reset();
            if (originMarker) createMap.removeLayer(originMarker);
            if (destMarker) createMap.removeLayer(destMarker);
            originMarker = null; destMarker = null;
            isSettingOrigin = true;
            setTimeout(function() {
                var modal = bootstrap.Modal.getInstance(document.getElementById('modalNovaRota'));
                if (modal) modal.hide();
                carregarRotas();
            }, 1200);
        } else {
            var err = await res.text();
            resultDiv.innerHTML = '<div class="p-3 rounded" style="background:var(--accent-red-bg);border:1px solid var(--accent-red);color:var(--accent-red);font-weight:600;">Erro: ' + escapeHtml(err) + '</div>';
        }
    } catch (err) {
        document.getElementById('routeResult').innerHTML = '<div class="p-3 rounded" style="background:var(--accent-red-bg);border:1px solid var(--accent-red);color:var(--accent-red);font-weight:600;">Erro de rede</div>';
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
});

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

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}

carregarRotas();
