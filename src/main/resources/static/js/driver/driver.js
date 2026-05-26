function getCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

const token = getCookie('token');
const driverId = getCookie('driverId');

let selectedBusId = null;
let selectedBus = null;
let selectedRouteId = null;
let selectedRoute = null;

let driverMap = null;
let driverRouteLine = null;
let driverDepMarker = null;
let driverDestMarker = null;

document.addEventListener('DOMContentLoaded', async () => {
    if (!userId || !token || !driverId || driverId === "" || driverId === "null") {
        window.location.href = '/auth/login';
        return;
    }

    const today = new Date();
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    document.getElementById('currentDate').textContent =
        today.toLocaleDateString('pt-BR', options).replace(/^./, function(m) { return m.toUpperCase(); });

    await atualizarAvatar();
    await loadBuses();
    setupEventos();
});

async function atualizarAvatar() {
    try {
        const res = await fetch('/user/' + userId, {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!res.ok) return;
        const data = await res.json();
        const avatar = document.getElementById('driverAvatar');
        if (data.userImageUrl && data.userImageUrl !== 'padrao') {
            avatar.style.background = 'url(' + data.userImageUrl + ') center/cover no-repeat';
            avatar.textContent = '';
        } else {
            avatar.style.background = 'var(--primary-orange)';
            const name = data.fullName || 'Motorista';
            const parts = name.trim().split(/\s+/);
            const initials = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0].substring(0, 2);
            avatar.textContent = initials.toUpperCase();
        }
    } catch (e) {
        console.error('Erro ao carregar avatar:', e);
    }
}

async function loadBuses() {
    const busesList = document.getElementById('busesList');
    busesList.innerHTML = '<div class="empty-state">Carregando ônibus...</div>';

    try {
        const res = await fetch('/api/driver/' + driverId + '/buses', {
            headers: { 'Authorization': 'Bearer ' + token }
        });

        if (!res.ok) {
            if (res.status === 403) { window.location.href = '/auth/login'; return; }
            throw new Error('Erro ' + res.status);
        }

        const buses = await res.json();
        renderizarBuses(buses);

        document.getElementById('loadingView').style.display = 'none';
        document.getElementById('mainView').style.display = 'block';

    } catch (err) {
        busesList.innerHTML = '<div class="empty-state">Erro ao carregar ônibus: ' + escapeHtml(err.message) + '</div>';
        document.getElementById('loadingView').innerHTML = `
            <div class="text-center">
                <p class="text-danger">Erro ao carregar: ${err.message}</p>
                <button class="btn btn-sm" style="background:var(--primary-orange);color:white;border:none;border-radius:8px;padding:8px 20px;" onclick="location.reload()">Tentar novamente</button>
            </div>
        `;
    }
}

function renderizarBuses(buses) {
    const container = document.getElementById('busesList');

    if (!buses || buses.length === 0) {
        container.innerHTML = '<div class="empty-state">Nenhum ônibus atribuído a você</div>';
        return;
    }

    container.innerHTML = '';
    buses.forEach(function(bus, index) {
        const div = document.createElement('div');
        div.className = 'bus-item d-flex justify-content-between align-items-center';
        div.setAttribute('data-bus-id', bus.idBus);

        const sign = bus.sign || 'Sem placa';
        const brand = bus.brand || '';
        const model = bus.model || '';
        const code = bus.code || '';

        div.innerHTML =
            '<div style="flex:1;min-width:0">' +
                '<div class="bus-name">' + escapeHtml(sign) + '</div>' +
                '<div class="bus-meta">' + escapeHtml(brand + ' ' + model) + (bus.color ? ' &bull; ' + escapeHtml(bus.color) : '') + '</div>' +
            '</div>' +
            '<div class="custom-radio"></div>';

        div.addEventListener('click', function() {
            selecionarOnibus(bus, this);
        });

        container.appendChild(div);
    });
}

function selecionarOnibus(bus, element) {
    document.querySelectorAll('.bus-item').forEach(function(el) {
        el.classList.remove('selected');
    });
    if (element) element.classList.add('selected');

    selectedBusId = bus.idBus;
    selectedBus = bus;

    const sign = bus.sign || 'Sem placa';
    const brand = bus.brand || '';
    const model = bus.model || '';

    document.getElementById('statBusName').textContent = sign;
    document.getElementById('statBusMeta').textContent = (brand + ' ' + model).trim();
    document.getElementById('statStatus').textContent = 'Selecionando...';
    document.getElementById('statStatusText').textContent = 'Carregando rotas';
    document.getElementById('btnBackToBuses').style.display = 'inline-block';

    selectedRouteId = null;
    selectedRoute = null;
    atualizarStatusPosSelecao();
    loadRoutes(selectedBusId);
}

async function loadRoutes(busId) {
    const routesList = document.getElementById('routesList');
    routesList.innerHTML = '<div class="empty-state">Carregando rotas...</div>';

    try {
        const res = await fetch('/api/driver/' + driverId + '/buses/' + busId + '/routes', {
            headers: { 'Authorization': 'Bearer ' + token }
        });

        if (!res.ok) throw new Error('Erro ' + res.status);
        const routes = await res.json();
        renderizarRoutes(routes);

        if (routes.length > 0) {
            document.getElementById('statStatus').textContent = 'Pronto';
            document.getElementById('statStatusText').textContent = 'Selecione uma rota';
        } else {
            document.getElementById('statStatus').textContent = 'Atenção';
            document.getElementById('statStatusText').textContent = 'Nenhuma rota';
        }

    } catch (err) {
        routesList.innerHTML = '<div class="empty-state">Erro ao carregar rotas</div>';
        console.error(err);
    }
}

function renderizarRoutes(routes) {
    const container = document.getElementById('routesList');

    if (!routes || routes.length === 0) {
        container.innerHTML = '<div class="empty-state">Nenhuma rota para este ônibus</div>';
        return;
    }

    container.innerHTML = '';
    routes.forEach(function(route) {
        const div = document.createElement('div');
        div.className = 'route-item d-flex justify-content-between align-items-center';
        div.setAttribute('data-route-id', route.idRoute);

        const origin = route.departurePoint || 'Origem';
        const destiny = route.destiny || 'Destino';
        const departure = route.departureTime || '--:--';
        const arrival = route.destinationTime || '--:--';
        const duration = route.estimatedDuration || '';
        const code = route.code || '';

        div.innerHTML =
            '<div style="flex:1;min-width:0">' +
                '<div class="route-name">' + escapeHtml(origin) + ' → ' + escapeHtml(destiny) + '</div>' +
                '<div class="route-meta">' + escapeHtml(departure) + ' → ' + escapeHtml(arrival) + (duration ? ' &bull; ' + escapeHtml(duration) : '') + '</div>' +
                (code ? '<span class="route-code">' + escapeHtml(code) + '</span>' : '') +
            '</div>' +
            '<div class="custom-radio"></div>';

        div.addEventListener('click', function() {
            selecionarRota(route, this);
        });

        container.appendChild(div);
    });
}

function selecionarRota(route, element) {
    document.querySelectorAll('.route-item').forEach(function(el) {
        el.classList.remove('selected');
    });
    if (element) element.classList.add('selected');

    selectedRouteId = route.idRoute;
    selectedRoute = route;

    const origin = route.departurePoint || 'Origem';
    const destiny = route.destiny || 'Destino';
    const departure = route.departureTime || '--:--';
    const code = route.code || '';

    document.getElementById('statRouteName').textContent = origin + ' → ' + destiny;
    document.getElementById('statRouteMeta').textContent = 'Saída: ' + departure;
    document.getElementById('statStatus').textContent = 'Rota definida';
    document.getElementById('statStatusText').textContent = 'Pronto para iniciar';

    if (code) {
        document.getElementById('routeCode').style.display = 'block';
        document.getElementById('routeCode').querySelector('.code-display').textContent = code;
    } else {
        document.getElementById('routeCode').style.display = 'none';
    }

    atualizarStatusPosSelecao();
    desenharMapa(route);
}

function backToBuses() {
    selectedRouteId = null;
    selectedRoute = null;
    selectedBusId = null;
    selectedBus = null;

    document.querySelectorAll('.bus-item').forEach(function(el) { el.classList.remove('selected'); });
    document.querySelectorAll('.route-item').forEach(function(el) { el.classList.remove('selected'); });

    document.getElementById('btnBackToBuses').style.display = 'none';
    document.getElementById('routesList').innerHTML = '<div class="empty-state">Selecione um ônibus primeiro</div>';

    document.getElementById('statBusName').textContent = '--';
    document.getElementById('statBusMeta').textContent = '';
    document.getElementById('statRouteName').textContent = '--';
    document.getElementById('statRouteMeta').textContent = '';
    document.getElementById('statStatus').textContent = 'Pronto';
    document.getElementById('statStatusText').textContent = 'Selecione ônibus e rota';
    document.getElementById('routeCode').style.display = 'none';

    if (driverMap) {
        driverMap.remove();
        driverMap = null;
        driverRouteLine = null;
        driverDepMarker = null;
        driverDestMarker = null;
    }

    atualizarStatusPosSelecao();
}

function atualizarStatusPosSelecao() {
    const btn = document.getElementById('btnIniciarRota');
    if (selectedBusId && selectedRouteId) {
        btn.disabled = false;
    } else {
        btn.disabled = true;
    }
}

function makeMiniIcon(color) {
    return L.divIcon({
        html: '<div style="background:' + color + ';width:20px;height:20px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.4);"></div>',
        iconSize: [20, 20], iconAnchor: [10, 10], className: ''
    });
}

function desenharMapa(route) {
    if (!route || typeof L === 'undefined') return;

    var depLat = parseFloat(route.departureLatitude);
    var depLng = parseFloat(route.departureLongitude);
    var destLat = parseFloat(route.destinationLatitude);
    var destLng = parseFloat(route.destinationLongitude);

    var temCoords = !isNaN(depLat) && !isNaN(depLng) && !isNaN(destLat) && !isNaN(destLng) &&
        isFinite(depLat) && isFinite(depLng) && isFinite(destLat) && isFinite(destLng);

    if (!driverMap) {
        var latInicial = temCoords ? (depLat + destLat) / 2 : -15.8;
        var lngInicial = temCoords ? (depLng + destLng) / 2 : -47.9;
        var zoomInicial = temCoords ? 12 : 4;

        driverMap = L.map('driverMap', { zoomControl: true, attributionControl: true }).setView([latInicial, lngInicial], zoomInicial);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(driverMap);
    }

    if (driverDepMarker) driverMap.removeLayer(driverDepMarker);
    if (driverDestMarker) driverMap.removeLayer(driverDestMarker);
    if (driverRouteLine) driverMap.removeLayer(driverRouteLine);

    if (!temCoords) {
        return;
    }

    driverDepMarker = L.marker([depLat, depLng], { icon: makeMiniIcon('#00d28a') }).addTo(driverMap).bindPopup('Origem: ' + (route.departurePoint || 'Partida'));
    driverDestMarker = L.marker([destLat, destLng], { icon: makeMiniIcon('#ff5722') }).addTo(driverMap).bindPopup('Destino: ' + (route.destiny || 'Chegada'));

    var pts = [[depLat, depLng], [destLat, destLng]];
    var bounds = L.latLngBounds(pts);
    driverMap.fitBounds(bounds, { padding: [30, 30] });

    setTimeout(function() {
        if (driverMap) driverMap.invalidateSize();
    }, 100);

    var url = 'https://router.project-osrm.org/route/v1/driving/' + depLng + ',' + depLat + ';' + destLng + ',' + destLat + '?geometries=geojson&overview=full';
    fetch(url).then(function(r) { return r.json(); }).then(function(data) {
        if (data.code !== 'Ok' || !data.routes || !data.routes[0]) return;
        var coords = data.routes[0].geometry.coordinates;
        var latlngs = coords.map(function(c) { return [c[1], c[0]]; });
        if (driverRouteLine) driverMap.removeLayer(driverRouteLine);
        driverRouteLine = L.polyline(latlngs, { color: '#0088ff', weight: 5, opacity: 0.8 }).addTo(driverMap);
        try { driverMap.fitBounds(driverRouteLine.getBounds(), { padding: [30, 30] }); } catch(e) {}
    }).catch(function(err) {});
}

function setupEventos() {
    document.getElementById('btnIniciarRota').addEventListener('click', function() {
        if (!selectedBusId || !selectedRouteId) return;
        window.location.href = '/sendLocation?busId=' + selectedBusId + '&routeId=' + selectedRouteId;
    });

    document.getElementById('editImageInput').addEventListener('change', function() {
        const file = this.files[0];
        if (!file) return;
        const preview = document.getElementById('editImagePreview');
        preview.src = URL.createObjectURL(file);
        preview.style.display = 'block';
        preview.style.width = '100px';
        preview.style.height = '100px';
        preview.style.borderRadius = '50%';
        preview.style.objectFit = 'cover';
        preview.style.border = '3px solid var(--primary-orange)';
        preview.style.margin = '0 auto';
    });

    document.getElementById('btnSalvarPerfil').addEventListener('click', async function() {
        const btn = this;
        const originalText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Salvando...';

        try {
            let imageUrl = document.getElementById('editUserImageUrl').value || 'padrao';
            const fileInput = document.getElementById('editImageInput');
            if (fileInput.files.length > 0) {
                const formData = new FormData();
                formData.append('file', fileInput.files[0]);
                const uploadRes = await fetch('/api/upload/image', {
                    method: 'POST',
                    headers: { 'Authorization': 'Bearer ' + token },
                    body: formData
                });
                if (uploadRes.ok) {
                    const uploadData = await uploadRes.json();
                    imageUrl = uploadData.imageUrl;
                }
            }

            const params = new URLSearchParams();
            params.append('fullName', document.getElementById('editFullName').value);
            params.append('adress', document.getElementById('editAdress').value);
            params.append('city', document.getElementById('editCity').value);
            params.append('birthDate', document.getElementById('editBirthDate').value);
            params.append('userImageUrl', imageUrl);

            const updateRes = await fetch('/user/' + userId, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Authorization': 'Bearer ' + token
                },
                body: params.toString()
            });

            if (updateRes.ok) {
                alert('Perfil atualizado com sucesso!');
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
}

function editarPerfil() {
    const offcanvas = new bootstrap.Offcanvas(document.getElementById('offcanvasEditarPerfil'));
    carregarDadosPerfil();
    offcanvas.show();
}

async function carregarDadosPerfil() {
    try {
        const response = await fetch('/user/' + userId, {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!response.ok) return;
        const data = await response.json();
        document.getElementById('editFullName').value = data.fullName || '';
        document.getElementById('editAdress').value = data.adress || '';
        document.getElementById('editCity').value = data.city || '';
        if (data.birthday) {
            document.getElementById('editBirthDate').value = data.birthday;
        }
        const preview = document.getElementById('editImagePreview');
        if (data.userImageUrl && data.userImageUrl !== 'padrao') {
            preview.src = data.userImageUrl;
            preview.style.display = 'block';
            preview.style.width = '100px';
            preview.style.height = '100px';
            preview.style.borderRadius = '50%';
            preview.style.objectFit = 'cover';
            preview.style.border = '3px solid var(--primary-orange)';
            preview.style.margin = '0 auto';
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
    preview.style.border = '3px solid var(--primary-orange)';
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
