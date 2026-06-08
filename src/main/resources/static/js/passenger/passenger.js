function getCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

const token = getCookie('token');
let allRoutes = [];
let selectedRouteId = null;
let selectedPresenceType = 'vai_volta';
let miniMap = null;
let miniRouteLine = null;
let miniDepMarker = null;
let miniDestMarker = null;

document.addEventListener('DOMContentLoaded', async () => {
    if (!userId || !token) {
        window.location.href = '/auth/login';
        return;
    }

    const today = new Date();
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    document.getElementById('currentDate').textContent =
        today.toLocaleDateString('pt-BR', options).replace(/^./, function(m) { return m.toUpperCase(); });

    await carregarRotas();
    setupEventos();
    await atualizarAvatar();
});

async function atualizarAvatar() {
    try {
        const res = await fetch('/user/' + userId, {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!res.ok) return;
        const data = await res.json();
        const avatar = document.getElementById('passengerAvatar');
        if (data.userImageUrl && data.userImageUrl !== 'padrao') {
            avatar.style.background = 'url(' + data.userImageUrl + ') center/cover no-repeat';
            avatar.textContent = '';
        } else {
            avatar.style.background = 'var(--primary-teal)';
            const name = data.fullName || 'Usuário';
            const parts = name.trim().split(/\s+/);
            const initials = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0].substring(0, 2);
            avatar.textContent = initials.toUpperCase();
        }
    } catch (e) {
        console.error('Erro ao carregar avatar:', e);
    }
}

async function carregarRotas() {
    try {
        const response = await fetch('/api/passenger/' + userId + '/all-routes', {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!response.ok) {
            if (response.status === 403) {
                window.location.href = '/auth/login';
                return;
            }
            throw new Error('Erro ao carregar rotas');
        }
        allRoutes = await response.json();
        renderizarRotas(allRoutes);
    } catch (error) {
        console.error('Erro:', error);
        document.getElementById('routeList').innerHTML = '<div class="route-empty">Erro ao carregar rotas.</div>';
    }
}

function renderizarRotas(routes) {
    const container = document.getElementById('routeList');

    if (!routes || routes.length === 0) {
        container.innerHTML = '<div class="route-empty">Nenhuma rota encontrada.<br>Use o menu acima para adicionar rotas.</div>';
        return;
    }

    container.innerHTML = '';
    routes.forEach(function(route, index) {
        const div = document.createElement('div');
        const origem = route.departurePoint || route.busModel || 'Origem';
        div.className = 'route-option d-flex justify-content-between align-items-center' + (index === 0 ? ' active' : '');
        div.setAttribute('data-route-id', route.idRoute);
        div.innerHTML =
            '<div style="flex:1;min-width:0">' +
                '<h5 class="fw-bold mb-1 fs-6">' + escapeHtml(origem) + ' <i class="bi bi-arrow-right text-muted-custom mx-1"></i> ' + escapeHtml(route.destiny || 'Destino') + '</h5>' +
                '<div class="text-muted-custom" style="font-size: 0.85rem;">' + (route.departurePoint ? escapeHtml(route.departurePoint) + ' &bull; ' : '') + (route.estimatedDuration || '') + '</div>' +
                (route.departureTime ? '<div class="text-muted-custom" style="font-size: 0.85rem;">Saída: ' + route.departureTime + (route.estimatedDuration ? ' &bull; ' + route.estimatedDuration : '') + '</div>' : '') +
            '</div>' +
            '<div class="d-flex align-items-center gap-2">' +
                '<button class="btn btn-sm p-1 text-danger" onclick="event.stopPropagation(); excluirRota(' + route.idRoute + ')" title="Remover rota" style="background:none;border:none;font-size:1.1rem;line-height:1;"><i class="bi bi-trash"></i></button>' +
                '<div class="custom-radio"></div>' +
            '</div>';
        container.appendChild(div);
    });

    const firstRoute = routes[0];
    selectedRouteId = firstRoute.idRoute;
    atualizarInfoCards(firstRoute);
}

function setupEventos() {
    document.getElementById('routeList').addEventListener('click', function(e) {
        const option = e.target.closest('.route-option');
        if (!option) return;

        document.querySelectorAll('.route-option').forEach(function(opt) {
            opt.classList.remove('active');
        });
        option.classList.add('active');

        selectedRouteId = option.getAttribute('data-route-id');
        const route = allRoutes.find(function(r) { return r.idRoute == selectedRouteId; });
        if (route) atualizarInfoCards(route);
    });

    document.querySelectorAll('.btn-presence').forEach(function(button) {
        button.addEventListener('click', function() {
            document.querySelectorAll('.btn-presence').forEach(function(btn) {
                btn.classList.remove('active');
            });
            this.classList.add('active');
            selectedPresenceType = this.getAttribute('data-presence-type');
        });
    });

    document.getElementById('btnConfirmar').addEventListener('click', confirmarPresenca);
}

function atualizarInfoCards(route) {
    document.getElementById('infoEmbarqueNome').textContent = route.departurePoint || '--';
    document.getElementById('infoEmbarqueEnd').textContent = route.departureAddress || '';
    document.getElementById('infoDesembarqueNome').textContent = route.destiny || '--';
    document.getElementById('infoDesembarqueEnd').textContent = route.destinationAddress || '';
    document.getElementById('infoSaidaHora').textContent = route.departureTime || '--';
    document.getElementById('infoSaidaDuracao').textContent = route.estimatedDuration || '';
    desenharMiniMapa(route);
}

async function confirmarPresenca() {
    if (!selectedRouteId) {
        alert('Selecione uma rota primeiro.');
        return;
    }

    const btn = document.getElementById('btnConfirmar');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Enviando...';
    btn.disabled = true;

    try {
        const response = await fetch('/api/passenger/' + userId + '/confirm-presence', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({
                routeId: parseInt(selectedRouteId),
                presenceType: selectedPresenceType
            })
        });

        if (response.ok) {
            window.location.href = '/passenger/map?routeId=' + selectedRouteId;
        } else {
            alert('Erro ao confirmar presença. Tente novamente.');
        }
    } catch (error) {
        console.error('Erro:', error);
        alert('Erro de conexão. Verifique sua internet e tente novamente.');
    } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}

async function excluirRota(routeId) {
    if (!confirm('Tem certeza que deseja remover esta rota?')) return;

    try {
        const response = await fetch('/api/passenger/' + userId + '/routes/' + routeId, {
            method: 'DELETE',
            headers: { 'Authorization': 'Bearer ' + token }
        });

        if (response.ok) {
            await carregarRotas();
        } else {
            alert('Erro ao remover rota. Tente novamente.');
        }
    } catch (error) {
        console.error('Erro:', error);
        alert('Erro de conexão. Verifique sua internet e tente novamente.');
    }
}

function abrirModalAdicionarRota() {
    document.getElementById('inputCodigoRota').value = '';
    document.getElementById('msgErroRota').classList.add('d-none');
    const modal = new bootstrap.Modal(document.getElementById('modalAdicionarRota'));
    modal.show();

    document.getElementById('btnAdicionarRota').onclick = async function() {
        const codigo = document.getElementById('inputCodigoRota').value.trim().toUpperCase();
        if (!codigo) {
            document.getElementById('msgErroRota').textContent = 'Digite um código.';
            document.getElementById('msgErroRota').classList.remove('d-none');
            return;
        }

        document.getElementById('btnAdicionarRota').disabled = true;
        document.getElementById('btnAdicionarRota').innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Adicionando...';

        try {
            const response = await fetch('/api/passenger/' + userId + '/subscribe-by-route', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token
                },
                body: JSON.stringify({ code: codigo })
            });

            if (response.ok) {
                modal.hide();
                await carregarRotas();
                alert('Rota adicionada com sucesso!');
            } else {
                const err = await response.text();
                document.getElementById('msgErroRota').textContent = err || 'Código inválido.';
                document.getElementById('msgErroRota').classList.remove('d-none');
            }
        } catch (error) {
            document.getElementById('msgErroRota').textContent = 'Erro de conexão.';
            document.getElementById('msgErroRota').classList.remove('d-none');
        } finally {
            document.getElementById('btnAdicionarRota').disabled = false;
            document.getElementById('btnAdicionarRota').innerHTML = 'Adicionar';
        }
    };
}

function abrirModalVisualizarRotas() {
    const container = document.getElementById('listaRotasVisualizar');

    if (!allRoutes || allRoutes.length === 0) {
        container.innerHTML = '<div class="text-muted-custom text-center py-3">Nenhuma rota cadastrada.</div>';
    } else {
        container.innerHTML = '';
        allRoutes.forEach(function(route) {
            const item = document.createElement('div');
            item.className = 'list-group-item d-flex justify-content-between align-items-center';
            item.style.cursor = 'pointer';
            item.innerHTML =
                '<div style="flex:1;min-width:0" onclick="this.closest(\'.list-group-item\').click()">' +
                    '<strong>' + escapeHtml(route.departurePoint || 'Origem') + ' → ' + escapeHtml(route.destiny) + '</strong><br>' +
                    '<small class="text-muted-custom">' + (route.departureTime ? 'Saída: ' + route.departureTime : '') + (route.estimatedDuration ? ' • ' + route.estimatedDuration : '') + '</small>' +
                '</div>' +
                '<div class="d-flex align-items-center gap-2">' +
                    '<button class="btn btn-sm text-danger p-1" onclick="event.stopPropagation(); excluirRota(' + route.idRoute + ')" title="Remover rota" style="background:none;border:none;font-size:1.1rem;"><i class="bi bi-trash"></i></button>' +
                    '<span class="badge bg-secondary">' + escapeHtml(route.busSign || '') + '</span>' +
                '</div>';
            item.addEventListener('click', function() {
                const modalEl = document.getElementById('modalVisualizarRotas');
                const modalInstance = bootstrap.Modal.getInstance(modalEl);
                if (modalInstance) modalInstance.hide();
                window.location.href = '/passenger/map?routeId=' + route.idRoute;
            });
            container.appendChild(item);
        });
    }

    const modal = new bootstrap.Modal(document.getElementById('modalVisualizarRotas'));
    modal.show();
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
            preview.style.border = '3px solid var(--primary-teal)';
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
    preview.style.border = '3px solid var(--primary-teal)';
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

function sair() {
    document.cookie = 'token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'userId=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'driverId=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'role=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    window.location.href = '/auth/login';
}

function makeMiniIcon(color) {
    return L.divIcon({
        html: '<div style="background:' + color + ';width:16px;height:16px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,.3);"></div>',
        iconSize: [16, 16], iconAnchor: [8, 8], className: ''
    });
}

function desenharMiniMapa(route) {
    if (!route || typeof L === 'undefined') return;
    var depLat = Number(route.departureLatitude);
    var depLng = Number(route.departureLongitude);
    var destLat = Number(route.destinationLatitude);
    var destLng = Number(route.destinationLongitude);
    if (!isFinite(depLat) || !isFinite(depLng) || !isFinite(destLat) || !isFinite(destLng)) return;

    var midLat = (depLat + destLat) / 2;
    var midLng = (depLng + destLng) / 2;

    if (!miniMap) {
        miniMap = L.map('miniMap', { zoomControl: false, attributionControl: false }).setView([midLat, midLng], 13);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(miniMap);
    }

    if (miniDepMarker) miniMap.removeLayer(miniDepMarker);
    if (miniDestMarker) miniMap.removeLayer(miniDestMarker);

    miniDepMarker = L.marker([depLat, depLng], { icon: makeMiniIcon('#00d28a') }).addTo(miniMap).bindPopup('Embarque');
    miniDestMarker = L.marker([destLat, destLng], { icon: makeMiniIcon('#ff5722') }).addTo(miniMap).bindPopup('Desembarque');

    var pts = [];
    pts.push([depLat, depLng]);
    pts.push([destLat, destLng]);

    var bounds = L.latLngBounds(pts);
    miniMap.fitBounds(bounds, { padding: [30, 30] });

    var url = 'https://router.project-osrm.org/route/v1/driving/' + depLng + ',' + depLat + ';' + destLng + ',' + destLat + '?geometries=geojson&overview=full';
    fetch(url).then(function(r) { return r.json(); }).then(function(data) {
        if (data.code !== 'Ok' || !data.routes || !data.routes[0]) return;
        var coords = data.routes[0].geometry.coordinates;
        var latlngs = coords.map(function(c) { return [c[1], c[0]]; });
        if (miniRouteLine) miniMap.removeLayer(miniRouteLine);
        miniRouteLine = L.polyline(latlngs, { color: '#0088ff', weight: 4, opacity: 0.7 }).addTo(miniMap);
        miniMap.fitBounds(miniRouteLine.getBounds(), { padding: [30, 30] });
    }).catch(function(err) {});
}

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}
