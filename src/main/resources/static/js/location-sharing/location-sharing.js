function getCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

const token = getCookie('token');
let driverId = getCookie('driverId');

let selectedBusId = null;
let selectedRouteId = null;
let tracking = false;
let lastSend = 0;

let trackingMap = null;
let routeLine = null;
let depMarker = null;
let destMarker = null;
let currentPosMarker = null;
let watchedRoute = null;
let mapInitialized = false;

const params = new URLSearchParams(window.location.search);
const initialBusId = params.get('busId');
const initialRouteId = params.get('routeId');

document.addEventListener('DOMContentLoaded', async () => {
    if (!userId || !token || !driverId || driverId === "" || driverId === "null") {
        window.location.href = '/auth/login';
        return;
    }

    const today = new Date();
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    const dateEl = document.getElementById('currentDateTracking');
    if (dateEl) {
        dateEl.textContent = today.toLocaleDateString('pt-BR', options).replace(/^./, m => m.toUpperCase());
    }

    await atualizarAvatar();
    setupEventos();

    if (initialBusId && initialRouteId) {
        selectedBusId = initialBusId;
        selectedRouteId = initialRouteId;
        await carregarDadosIniciais(initialBusId, initialRouteId);
        initMap();
        startTracking();
    } else {
        mostrarFallback();
        loadBuses();
    }
});

async function atualizarAvatar() {
    try {
        const res = await fetch('/user/' + userId, {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!res.ok) return;
        const data = await res.json();
        const avatar = document.getElementById('trackingAvatar');
        if (!avatar) return;
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

async function carregarDadosIniciais(busId, routeId) {
    try {
        const [busRes, routeRes] = await Promise.all([
            fetch('/api/driver/' + driverId + '/buses', { headers: { 'Authorization': 'Bearer ' + token } }),
            fetch('/api/driver/' + driverId + '/buses/' + busId + '/routes', { headers: { 'Authorization': 'Bearer ' + token } })
        ]);

        let busName = 'Ônibus';
        let routeName = 'Rota';
        let routeCode = '------';

        if (busRes.ok) {
            const buses = await busRes.json();
            const bus = buses.find(b => String(b.idBus) === String(busId));
            if (bus) {
                busName = (bus.sign || 'Sem placa') + ' (' + (bus.code || '') + ')';
            }
        }

        if (routeRes.ok) {
            const routes = await routeRes.json();
            const route = routes.find(r => String(r.idRoute) === String(routeId));
            if (route) {
                watchedRoute = route;
                const origin = route.departurePoint || 'Origem';
                const destiny = route.destiny || 'Destino';
                routeName = origin + ' → ' + destiny;
                routeCode = route.code || '------';
            }
        }

        atualizarChips(busName, routeName, routeCode);

    } catch (err) {
        console.error('Erro ao carregar dados iniciais:', err);
    }
}

function atualizarChips(busName, routeName, routeCode) {
    const elBus = document.getElementById('chipBusName');
    const elRoute = document.getElementById('chipRouteName');
    const elCode = document.getElementById('chipCode');

    if (elBus) elBus.textContent = busName || '--';
    if (elRoute) elRoute.textContent = routeName || '--';
    if (elCode) elCode.textContent = routeCode || '------';
}

function initMap() {
    if (mapInitialized || typeof L === 'undefined') return;

    const mapEl = document.getElementById('trackingMap');
    if (!mapEl) return;

    trackingMap = L.map('trackingMap', { zoomControl: true, attributionControl: true }).setView([-15.8, -47.9], 4);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(trackingMap);

    mapInitialized = true;

    if (watchedRoute) {
        desenharRotaNoMapa(watchedRoute);
    }
}

function makeMarkerIcon(color, size) {
    const s = size || 24;
    return L.divIcon({
        html: '<div style="background:' + color + ';width:' + s + 'px;height:' + s + 'px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 10px rgba(0,0,0,.4);"></div>',
        iconSize: [s, s], iconAnchor: [s / 2, s / 2], className: ''
    });
}

function makeBusIcon() {
    return L.divIcon({
        html: `<div style="position:relative;">
            <div style="width:36px;height:36px;background:var(--primary-orange);border-radius:50%;display:flex;align-items:center;justify-content:center;border:3px solid white;box-shadow:0 3px 12px rgba(0,0,0,.4);">
                <i class="bi bi-bus-front-fill" style="color:white;font-size:18px;"></i>
            </div>
            <div style="position:absolute;top:-2px;left:-2px;right:-2px;bottom:-2px;border-radius:50%;border:2px solid var(--primary-orange);animation:ping 1.5s infinite;"></div>
        </div>
        <style>
            @keyframes ping {
                0% { transform:scale(1); opacity:1; }
                70% { transform:scale(1.3); opacity:0; }
                100% { transform:scale(1.3); opacity:0; }
            }
        </style>`,
        iconSize: [36, 36], iconAnchor: [18, 18], className: ''
    });
}

function desenharRotaNoMapa(route) {
    if (!trackingMap || !route) return;

    var depLat = parseFloat(route.departureLatitude);
    var depLng = parseFloat(route.departureLongitude);
    var destLat = parseFloat(route.destinationLatitude);
    var destLng = parseFloat(route.destinationLongitude);

    var temCoords = !isNaN(depLat) && !isNaN(depLng) && !isNaN(destLat) && !isNaN(destLng) &&
        isFinite(depLat) && isFinite(depLng) && isFinite(destLat) && isFinite(destLng);

    if (depMarker) trackingMap.removeLayer(depMarker);
    if (destMarker) trackingMap.removeLayer(destMarker);
    if (routeLine) trackingMap.removeLayer(routeLine);

    if (!temCoords) return;

    depMarker = L.marker([depLat, depLng], { icon: makeMarkerIcon('#00d28a', 20) }).addTo(trackingMap).bindPopup('Origem: ' + (route.departurePoint || 'Partida'));
    destMarker = L.marker([destLat, destLng], { icon: makeMarkerIcon('#ff5722', 20) }).addTo(trackingMap).bindPopup('Destino: ' + (route.destiny || 'Chegada'));

    var pts = [[depLat, depLng], [destLat, destLng]];
    var bounds = L.latLngBounds(pts);
    trackingMap.fitBounds(bounds, { padding: [40, 40] });

    setTimeout(function() {
        if (trackingMap) trackingMap.invalidateSize();
    }, 200);

    var url = 'https://router.project-osrm.org/route/v1/driving/' + depLng + ',' + depLat + ';' + destLng + ',' + destLat + '?geometries=geojson&overview=full';
    fetch(url).then(r => r.json()).then(function(data) {
        if (data.code !== 'Ok' || !data.routes || !data.routes[0]) return;
        var coords = data.routes[0].geometry.coordinates;
        var latlngs = coords.map(c => [c[1], c[0]]);
        if (routeLine) trackingMap.removeLayer(routeLine);
        routeLine = L.polyline(latlngs, { color: '#0088ff', weight: 5, opacity: 0.8 }).addTo(trackingMap);
        try { trackingMap.fitBounds(routeLine.getBounds(), { padding: [40, 40] }); } catch(e) {}
    }).catch(function(err) {});
}

function atualizarPosicaoNoMapa(lat, lng) {
    if (!trackingMap) return;

    if (currentPosMarker) {
        currentPosMarker.setLatLng([lat, lng]);
    } else {
        currentPosMarker = L.marker([lat, lng], { icon: makeBusIcon(), zIndexOffset: 1000 }).addTo(trackingMap);
        trackingMap.setView([lat, lng], 14);
    }

    const coordEl = document.getElementById('coordText');
    if (coordEl) {
        coordEl.textContent = lat.toFixed(5) + ', ' + lng.toFixed(5);
    }
}

function mostrarFallback() {
    const fallback = document.getElementById('fallbackView');
    const topbar = document.querySelector('.tracking-topbar');
    const infoBar = document.querySelector('.info-bar');
    const map = document.getElementById('trackingMap');
    const bottomBar = document.getElementById('bottomStatusBar');

    if (fallback) fallback.style.display = 'flex';
    if (topbar) topbar.style.display = 'none';
    if (infoBar) infoBar.style.display = 'none';
    if (map) map.style.display = 'none';
    if (bottomBar) bottomBar.style.display = 'none';
}

function ocultarFallback() {
    const fallback = document.getElementById('fallbackView');
    const topbar = document.querySelector('.tracking-topbar');
    const infoBar = document.querySelector('.info-bar');
    const map = document.getElementById('trackingMap');
    const bottomBar = document.getElementById('bottomStatusBar');

    if (fallback) fallback.style.display = 'none';
    if (topbar) topbar.style.display = 'block';
    if (infoBar) infoBar.style.display = 'block';
    if (map) map.style.display = 'block';
    if (bottomBar) bottomBar.style.display = 'block';
}

async function loadBuses() {
    const busSelect = document.getElementById('busSelect');
    if (!busSelect) return;
    busSelect.innerHTML = '<option value="">Carregando...</option>';

    try {
        const res = await fetch('/api/driver/' + driverId + '/buses', {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!res.ok) throw new Error('Erro ' + res.status);
        const buses = await res.json();

        busSelect.innerHTML = '<option value="">Selecione um ônibus</option>';
        buses.forEach(b => {
            const opt = document.createElement('option');
            opt.value = b.idBus;
            opt.textContent = (b.sign || 'Sem placa') + ' (' + (b.brand || '') + ' ' + (b.model || '') + ') - ' + (b.code || '');
            opt.dataset.sign = b.sign || '';
            opt.dataset.code = b.code || '';
            opt.dataset.brand = b.brand || '';
            opt.dataset.model = b.model || '';
            busSelect.appendChild(opt);
        });

    } catch (err) {
        busSelect.innerHTML = '<option value="">Erro ao carregar ônibus</option>';
        console.error(err);
    }
}

async function loadRoutes(busId) {
    const routeSelect = document.getElementById('routeSelect');
    const codeSection = document.getElementById('codeSection');
    const btnStart = document.getElementById('btnStartManual');

    if (!routeSelect) return;
    routeSelect.disabled = true;
    routeSelect.innerHTML = '<option value="">Carregando...</option>';
    if (codeSection) codeSection.style.display = 'none';
    if (btnStart) btnStart.disabled = true;

    try {
        const res = await fetch('/api/driver/' + driverId + '/buses/' + busId + '/routes', {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!res.ok) throw new Error('Erro ' + res.status);
        const routes = await res.json();

        routeSelect.innerHTML = '<option value="">Selecione uma rota</option>';
        routes.forEach(r => {
            const opt = document.createElement('option');
            opt.value = r.idRoute;
            const origin = r.departurePoint || 'Origem';
            const destiny = r.destiny || 'Destino';
            opt.textContent = origin + ' → ' + destiny + ' (' + (r.code || '') + ')';
            opt.dataset.code = r.code || '';
            opt.dataset.origin = origin;
            opt.dataset.destiny = destiny;
            opt.dataset.departureTime = r.departureTime || '';
            opt.dataset.departureLatitude = r.departureLatitude || '';
            opt.dataset.departureLongitude = r.departureLongitude || '';
            opt.dataset.destinationLatitude = r.destinationLatitude || '';
            opt.dataset.destinationLongitude = r.destinationLongitude || '';
            routeSelect.appendChild(opt);
        });

        routeSelect.disabled = routes.length === 0;

    } catch (err) {
        routeSelect.innerHTML = '<option value="">Erro ao carregar rotas</option>';
        console.error(err);
    }
}

function setupEventos() {
    const busSelect = document.getElementById('busSelect');
    const routeSelect = document.getElementById('routeSelect');
    const btnStart = document.getElementById('btnStartManual');

    if (busSelect) {
        busSelect.addEventListener('change', function() {
            selectedBusId = this.value;
            selectedRouteId = null;
            if (selectedBusId) {
                loadRoutes(selectedBusId);
            } else {
                if (routeSelect) {
                    routeSelect.innerHTML = '<option value="">Primeiro selecione um ônibus</option>';
                    routeSelect.disabled = true;
                }
                const codeSection = document.getElementById('codeSection');
                if (codeSection) codeSection.style.display = 'none';
                if (btnStart) btnStart.disabled = true;
            }
        });
    }

    if (routeSelect) {
        routeSelect.addEventListener('change', function() {
            selectedRouteId = this.value;
            const codeDisplay = document.getElementById('codeDisplay');
            const codeSection = document.getElementById('codeSection');

            if (selectedRouteId && this.options[this.selectedIndex]) {
                const opt = this.options[this.selectedIndex];
                const code = opt.dataset.code || '------';
                if (codeDisplay) codeDisplay.textContent = code;
                if (codeSection) codeSection.style.display = 'block';
                if (btnStart) btnStart.disabled = false;

                watchedRoute = {
                    idRoute: selectedRouteId,
                    code: code,
                    departurePoint: opt.dataset.origin || 'Origem',
                    destiny: opt.dataset.destiny || 'Destino',
                    departureTime: opt.dataset.departureTime || '',
                    departureLatitude: opt.dataset.departureLatitude || null,
                    departureLongitude: opt.dataset.departureLongitude || null,
                    destinationLatitude: opt.dataset.destinationLatitude || null,
                    destinationLongitude: opt.dataset.destinationLongitude || null
                };

                if (busSelect && busSelect.options[busSelect.selectedIndex]) {
                    const busOpt = busSelect.options[busSelect.selectedIndex];
                    const busName = (busOpt.dataset.sign || 'Ônibus') + ' (' + (busOpt.dataset.code || '') + ')';
                    const routeName = (opt.dataset.origin || 'Origem') + ' → ' + (opt.dataset.destiny || 'Destino');
                    atualizarChips(busName, routeName, code);
                }

            } else {
                if (codeSection) codeSection.style.display = 'none';
                if (btnStart) btnStart.disabled = true;
                watchedRoute = null;
            }
        });
    }

    if (btnStart) {
        btnStart.addEventListener('click', function() {
            if (!selectedBusId || !selectedRouteId) return;
            ocultarFallback();
            initMap();
            if (watchedRoute) {
                desenharRotaNoMapa(watchedRoute);
            }
            startTracking();
        });
    }

    const editImageInput = document.getElementById('editImageInputTrack');
    if (editImageInput) {
        editImageInput.addEventListener('change', function() {
            const file = this.files[0];
            if (!file) return;
            const preview = document.getElementById('editImagePreviewTrack');
            if (!preview) return;
            preview.src = URL.createObjectURL(file);
            preview.style.display = 'block';
            preview.style.width = '100px';
            preview.style.height = '100px';
            preview.style.borderRadius = '50%';
            preview.style.objectFit = 'cover';
            preview.style.border = '3px solid var(--primary-orange)';
            preview.style.margin = '0 auto';
        });
    }

    const btnSalvar = document.getElementById('btnSalvarPerfilTrack');
    if (btnSalvar) {
        btnSalvar.addEventListener('click', async function() {
            const btn = this;
            const originalText = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Salvando...';

            try {
                let imageUrl = document.getElementById('editUserImageUrlTrack').value || 'padrao';
                const fileInput = document.getElementById('editImageInputTrack');
                if (fileInput && fileInput.files.length > 0) {
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
                params.append('fullName', document.getElementById('editFullNameTrack').value || '');
                params.append('adress', document.getElementById('editAdressTrack').value || '');
                params.append('city', document.getElementById('editCityTrack').value || '');
                params.append('birthDate', document.getElementById('editBirthDateTrack').value || '');
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
}

function startTracking() {
    if (!selectedBusId || !selectedRouteId) return;
    tracking = true;

    const statusText = document.getElementById('statusText');
    const statusDot = document.getElementById('gpsStatusDot');
    const gpsText = document.getElementById('gpsStatusText');

    if (statusText) statusText.textContent = 'Iniciando compartilhamento...';
    if (statusDot) statusDot.className = 'status-dot-small';
    if (gpsText) gpsText.textContent = 'Aguardando permissão...';

    const isSecure = location.protocol === 'https:' || location.hostname === 'localhost' || location.hostname === '127.0.0.1';
    if (!isSecure) {
        console.warn('Aviso: Geolocalização requer HTTPS ou localhost.');
    }

    if (!('geolocation' in navigator)) {
        const msg = 'Seu navegador não suporta geolocalização.';
        alert(msg);
        if (gpsText) gpsText.textContent = 'Não suportado';
        return;
    }

    try {
        navigator.permissions && navigator.permissions.query({ name: 'geolocation' }).then(function(result) {
            console.log('Permissão GPS:', result.state);
            if (result.state === 'denied') {
                alert('A permissão de localização foi negada. Por favor, habilite-a nas configurações do navegador.');
            }
        }).catch(function(err) {
            console.log('Não foi possível verificar permissão:', err);
        });
    } catch (e) {}

    navigator.geolocation.watchPosition(
        function(pos) {
            const now = Date.now();
            if (now - lastSend < 1000) return;
            lastSend = now;

            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;

            if (statusText) statusText.textContent = 'Compartilhando localização em tempo real...';
            if (statusDot) statusDot.className = 'status-dot-small active';
            if (gpsText) gpsText.textContent = 'Ativo';

            atualizarPosicaoNoMapa(lat, lng);
            sendToBackend(lat, lng);
        },
        function(err) {
            console.error('Erro GPS:', err.code, err.message);

            let errorMsg = 'Erro ao obter localização.';
            let displayText = 'Erro GPS';

            switch (err.code) {
                case 1:
                    errorMsg = 'Permissão de localização negada.\n\nPor favor, habilite a permissão de localização nas configurações do navegador.';
                    displayText = 'Permissão negada';
                    break;
                case 2:
                    errorMsg = 'Não foi possível obter sua localização (GPS indisponível).';
                    displayText = 'GPS indisponível';
                    break;
                case 3:
                    errorMsg = 'Tempo esgotado ao tentar obter localização.';
                    displayText = 'Timeout';
                    break;
            }

            alert(errorMsg);

            if (gpsText) gpsText.textContent = displayText;
            if (statusDot) statusDot.className = 'status-dot-small';
        },
        {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 0
        }
    );
}

function sendToBackend(lat, lon) {
    if (!tracking || !selectedBusId || !selectedRouteId) return;

    const url1 = '/api/driver/' + driverId + '/tracking/' + selectedBusId;
    const url2 = '/api/driver/' + driverId + '/routes/' + selectedRouteId + '/tracking';
    const body = JSON.stringify({ latitude: lat, longitude: lon });
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
    };

    fetch(url1, { method: 'POST', headers, body }).catch(err => console.error('Erro ao enviar (bus):', err));
    fetch(url2, { method: 'POST', headers, body }).catch(err => console.error('Erro ao enviar (route):', err));
}

function pararCompartilhamento() {
    if (confirm('Deseja realmente parar o compartilhamento de localização?')) {
        tracking = false;
        window.location.href = '/driver';
    }
}

function voltarAoPainel() {
    window.location.href = '/driver';
}

function editarPerfil() {
    const offcanvas = new bootstrap.Offcanvas(document.getElementById('offcanvasEditarPerfilTracking'));
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

        const elName = document.getElementById('editFullNameTrack');
        const elAdress = document.getElementById('editAdressTrack');
        const elCity = document.getElementById('editCityTrack');
        const elBirth = document.getElementById('editBirthDateTrack');
        const preview = document.getElementById('editImagePreviewTrack');
        const elUrl = document.getElementById('editUserImageUrlTrack');

        if (elName) elName.value = data.fullName || '';
        if (elAdress) elAdress.value = data.adress || '';
        if (elCity) elCity.value = data.city || '';
        if (elBirth && data.birthday) elBirth.value = data.birthday;
        if (elUrl && data.userImageUrl) elUrl.value = data.userImageUrl;

        if (preview) {
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
