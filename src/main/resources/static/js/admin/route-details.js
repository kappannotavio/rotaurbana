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

var routeId = window.location.pathname.split('/').pop();
var routeMap = null;
var depMarker = null;
var destMarker = null;
var routeLine = null;
var allPassengers = [];

function makeIcon(color) {
    return L.divIcon({
        html: '<div style="background:' + color + ';width:20px;height:20px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.4);"></div>',
        iconSize: [20, 20], iconAnchor: [10, 10], className: ''
    });
}

function initMap(route) {
    var depLat = Number(route.departureLatitude);
    var depLng = Number(route.departureLongitude);
    var destLat = Number(route.destinationLatitude);
    var destLng = Number(route.destinationLongitude);

    var hasDep = isFinite(depLat) && isFinite(depLng);
    var hasDest = isFinite(destLat) && isFinite(destLng);

    if (!hasDep && !hasDest) {
        document.getElementById('routeMap').innerHTML = '<div style="width:100%;height:100%;display:flex;align-items:center;justify-content:center;background:#1c1c21;color:#8d8d99;font-size:0.9rem;">Coordenadas da rota não disponíveis</div>';
        return;
    }

    if (!routeMap) {
        routeMap = L.map('routeMap', { zoomControl: true, attributionControl: false });
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(routeMap);
    }

    var bounds = [];

    if (hasDep) {
        if (depMarker) routeMap.removeLayer(depMarker);
        depMarker = L.marker([depLat, depLng], { icon: makeIcon('#00b37e') }).addTo(routeMap)
            .bindPopup('<b>Origem</b><br>' + (route.departurePoint || route.departureAddress || ''));
        bounds.push([depLat, depLng]);
    }

    if (hasDest) {
        if (destMarker) routeMap.removeLayer(destMarker);
        destMarker = L.marker([destLat, destLng], { icon: makeIcon('#ff5e00') }).addTo(routeMap)
            .bindPopup('<b>Destino</b><br>' + (route.destiny || route.destinationAddress || ''));
        bounds.push([destLat, destLng]);
    }

    if (routeLine) routeMap.removeLayer(routeLine);

    if (hasDep && hasDest) {
        var url = 'https://router.project-osrm.org/route/v1/driving/' + depLng + ',' + depLat + ';' + destLng + ',' + destLat + '?geometries=geojson&overview=full';
        fetch(url).then(function(r) { return r.json(); }).then(function(data) {
            if (data.code === 'Ok' && data.routes && data.routes[0]) {
                var coords = data.routes[0].geometry.coordinates;
                var latlngs = coords.map(function(c) { return [c[1], c[0]]; });
                routeLine = L.polyline(latlngs, { color: '#ff5e00', weight: 3, opacity: 0.6 }).addTo(routeMap);
                routeMap.fitBounds(routeLine.getBounds(), { padding: [30, 30] });
            }
        }).catch(function() {});
    }

    if (bounds.length > 0) {
        routeMap.fitBounds(L.latLngBounds(bounds), { padding: [30, 30], maxZoom: 15 });
    }
}

function paymentBadgeClass(status) {
    if (status === 'EM_DAY') return 'payment-em-dia';
    if (status === 'PENDING') return 'payment-pendente';
    return '';
}

function paymentLabel(status) {
    if (status === 'EM_DAY') return 'Em Dia';
    if (status === 'PENDING') return 'Pendente';
    return '';
}

async function carregarDetalhes() {
    try {
        var [routeRes, userRes] = await Promise.all([
            fetch('/api/admin/' + userId + '/routes/' + routeId + '/details', {
                headers: { 'Authorization': 'Bearer ' + token }
            }),
            fetch('/user/' + userId, {
                headers: { 'Authorization': 'Bearer ' + token }
            })
        ]);

        if (!routeRes.ok) {
            if (routeRes.status === 403) { window.location.href = '/auth/login'; return; }
            throw new Error('Erro ' + routeRes.status);
        }

        var data = await routeRes.json();
        renderizar(data);

        if (userRes.ok) {
            var userData = await userRes.json();
            atualizarAvatar(userData);
        }
    } catch (err) {
        document.getElementById('loadingView').innerHTML = '<div class="text-center"><p class="text-danger">Erro ao carregar dados: ' + err.message + '</p><button class="btn btn-custom-solid mt-2" onclick="location.reload()">Tentar novamente</button></div>';
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

function renderizar(data) {
    var route = data.route;
    var driver = data.driver;
    var passengers = data.passengers || [];
    var stats = data.stats || {};
    allPassengers = passengers;

    document.getElementById('routeTitle').textContent = (route.departurePoint || 'Origem') + ' \u2192 ' + (route.destiny || 'Destino');

    document.getElementById('statTotal').textContent = stats.totalPassengers || 0;
    document.getElementById('statEmDay').textContent = stats.emDayCount || 0;
    document.getElementById('statLate').textContent = stats.pendingCount || 0;

    var passengerContainer = document.getElementById('passengerList');
    passengerContainer.innerHTML = '';

    if (passengers.length === 0) {
        passengerContainer.innerHTML = '<div class="text-muted-custom text-center py-3">Nenhum passageiro inscrito nesta rota</div>';
    } else {
        passengers.forEach(function(p) {
            var div = document.createElement('div');
            div.className = 'info-card d-flex justify-content-between align-items-center m-0';

            var isAdminOrDriver = p.role === 'ADMIN' || p.role === 'DRIVER';
            var paymentHtml = isAdminOrDriver ? '' :
                '<div class="d-flex align-items-center gap-2">' +
                    '<span class="payment-badge ' + paymentBadgeClass(p.paymentStatus) + '">' + paymentLabel(p.paymentStatus) + '</span>' +
                '</div>';

            div.innerHTML =
                '<div>' +
                    '<div class="fw-bold" style="font-size: 0.9rem;">' + escapeHtml(p.fullName || 'Desconhecido') + '</div>' +
                    (p.presenceTypeLabel ? '<div class="text-muted-custom text-xs">' + escapeHtml(p.presenceTypeLabel) + '</div>' : '') +
                '</div>' +
                paymentHtml;
            passengerContainer.appendChild(div);
        });
    }

    if (driver) {
        document.getElementById('driverName').textContent = driver.fullName || '--';
        document.getElementById('driverLicence').textContent = 'CNH: ' + (driver.licence || '--');
    } else {
        document.getElementById('driverName').textContent = 'Sem motorista';
        document.getElementById('driverLicence').textContent = '--';
    }

    document.getElementById('originName').textContent = route.departurePoint || '--';
    document.getElementById('originTime').textContent = route.departureTime ? 'Sa\u00EDda ' + route.departureTime : '--';

    document.getElementById('stopsCount').textContent = passengers.length > 0 ? passengers.length : '0';
    document.getElementById('stopsDetail').textContent = route.estimatedDuration || '--';

    document.getElementById('destinyName').textContent = route.destiny || '--';
    document.getElementById('destinyTime').textContent = route.destinationTime ? 'Chegada ' + route.destinationTime : '--';

    if (route.departureAddress && document.getElementById('originTime')) {
        var addrEl = document.createElement('div');
        addrEl.className = 'text-muted-custom text-xs mt-1';
        addrEl.textContent = route.departureAddress;
        document.getElementById('originTime').parentNode.insertBefore(addrEl, document.getElementById('originTime').nextSibling);
    }

    document.getElementById('loadingView').style.display = 'none';
    document.getElementById('mainContent').style.display = 'block';

    if (typeof L !== 'undefined') {
        setTimeout(function() { initMap(route); }, 100);
    }
}

function abrirModalEditar() {
    var passengers = allPassengers;

    document.getElementById('editDeparturePoint').value = document.getElementById('originName').textContent !== '--' ? document.getElementById('originName').textContent : '';
    document.getElementById('editDestiny').value = document.getElementById('destinyName').textContent !== '--' ? document.getElementById('destinyName').textContent : '';
    document.getElementById('editDepartureTime').value = document.getElementById('originTime').textContent.replace('Sa\u00EDda ', '');
    document.getElementById('editDestinationTime').value = document.getElementById('destinyTime').textContent.replace('Chegada ', '');
    document.getElementById('editEstimatedDuration').value = document.getElementById('stopsDetail').textContent !== '--' ? document.getElementById('stopsDetail').textContent : '';

    var container = document.getElementById('editPassengerList');
    container.innerHTML = '';

    if (passengers.length === 0) {
        container.innerHTML = '<div class="text-muted-custom text-center py-3">Nenhum passageiro inscrito</div>';
    } else {
        passengers.forEach(function(p) {
            var isAdminOrDriver = p.role === 'ADMIN' || p.role === 'DRIVER';
            var card = document.createElement('div');
            card.className = 'user-card';

            var currentStatus = p.paymentStatus || 'EM_DAY';

            if (isAdminOrDriver) {
                card.innerHTML =
                    '<div class="user-name">' + escapeHtml(p.fullName) + '</div>' +
                    '<div class="text-muted-custom text-xs">---</div>';
            } else {
                card.innerHTML =
                    '<div class="user-name">' + escapeHtml(p.fullName) + '</div>' +
                    '<div class="status-group" data-user-id="' + p.id + '" data-status="' + currentStatus + '">' +
                        '<span class="status-btn em-dia' + (currentStatus === 'EM_DAY' ? ' active' : '') + '" data-value="EM_DAY" onclick="togglePaymentStatus(this)">Em Dia</span>' +
                        '<span class="status-btn pendente' + (currentStatus !== 'EM_DAY' ? ' active' : '') + '" data-value="PENDING" onclick="togglePaymentStatus(this)">Pendente</span>' +
                    '</div>';
            }
            container.appendChild(card);
        });
    }

    var modal = new bootstrap.Modal(document.getElementById('modalEditarRota'));
    modal.show();
}

function togglePaymentStatus(el) {
    var group = el.closest('.status-group');
    group.querySelectorAll('.status-btn').forEach(function(btn) { btn.classList.remove('active'); });
    el.classList.add('active');
    group.dataset.status = el.dataset.value;
}

document.getElementById('btnSalvarPagamentos').addEventListener('click', async function() {
    var btn = this;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Salvando...';

    var payments = [];
    document.querySelectorAll('.status-group').forEach(function(group) {
        payments.push({
            userId: parseInt(group.dataset.userId),
            paymentStatus: group.dataset.status
        });
    });

    try {
        var res = await fetch('/api/admin/' + userId + '/routes/' + routeId + '/payment', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ payments: payments })
        });

        if (res.ok) {
            var modal = bootstrap.Modal.getInstance(document.getElementById('modalEditarRota'));
            if (modal) modal.hide();
            await carregarDetalhes();
        } else {
            alert('Erro ao salvar pagamentos. Tente novamente.');
        }
    } catch (err) {
        alert('Erro de conexão.');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Salvar Alterações';
    }
});

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}

function getInitials(name) {
    if (!name) return 'AD';
    var parts = name.trim().split(/\s+/);
    return parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0].substring(0, 2);
}

function gerenciarRotas() {
    window.location.href = '/admin/routes';
}

function sair() {
    document.cookie = 'token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'userId=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'driverId=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'role=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    window.location.href = '/auth/login';
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
        if (data.birthday) {
            document.getElementById('editBirthDate').value = data.birthday;
        }
        var preview = document.getElementById('editImagePreview');
        if (data.userImageUrl && data.userImageUrl !== 'padrao') {
            preview.src = data.userImageUrl;
            preview.style.display = 'block';
        } else {
            preview.src = '';
            preview.style.display = 'none';
        }
        if (data.userImageUrl) {
            document.getElementById('editUserImageUrl').value = data.userImageUrl;
        }
    } catch (e) {
        console.error('Erro ao carregar perfil:', e);
    }
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
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Authorization': 'Bearer ' + token
            },
            body: params.toString()
        });

        if (updateRes.ok) {
            var offcanvas = bootstrap.Offcanvas.getInstance(document.getElementById('offcanvasEditarPerfil'));
            if (offcanvas) offcanvas.hide();
            location.reload();
        } else {
            alert('Erro ao atualizar perfil.');
        }
    } catch (e) {
        console.error('Erro:', e);
        alert('Erro de conexão.');
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
});

carregarDetalhes();
