document.addEventListener("DOMContentLoaded", () => {
    function getCookie(name) {
        const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
        return match ? match[2] : null;
    }

    const token = getCookie("token");
    const userId = getCookie("userId");

    // --- 1. Controle visual e de estado das Rotas ---
    const routeOptions = document.querySelectorAll('.route-option');
    let selectedRouteId = null;

    routeOptions.forEach(option => {
        if (option.classList.contains('active')) {
            selectedRouteId = option.getAttribute('data-route-id');
        }
        option.addEventListener('click', function() {
            routeOptions.forEach(opt => opt.classList.remove('active'));
            this.classList.add('active');
            selectedRouteId = this.getAttribute('data-route-id');
        });
    });

    // --- 2. Controle visual e de estado das Presenças ---
    const presenceButtons = document.querySelectorAll('.btn-presence');
    let selectedPresenceType = 'vai_volta';

    presenceButtons.forEach(button => {
        button.addEventListener('click', function() {
            presenceButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');
            selectedPresenceType = this.getAttribute('data-presence-type');
        });
    });

    // --- 3. Envio da Requisição ---
    const btnConfirmar = document.getElementById('btnConfirmar');

    btnConfirmar.addEventListener('click', async () => {
        if (!selectedRouteId) {
            alert("Selecione uma rota");
            return;
        }

        const originalText = btnConfirmar.innerHTML;
        btnConfirmar.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Enviando...';
        btnConfirmar.disabled = true;

        try {
            const response = await fetch(`/api/passenger/${userId}/confirm-presence`, {
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
                const data = await response.json();
                alert(`Presença confirmada com sucesso!`);
            } else {
                alert('Erro ao confirmar presença. O servidor retornou um erro.');
            }
        } catch (error) {
            console.error('Erro na requisição:', error);
            alert('Erro de conexão. Verifique sua internet e tente novamente.');
        } finally {
            btnConfirmar.innerHTML = originalText;
            btnConfirmar.disabled = false;
        }
    });
});