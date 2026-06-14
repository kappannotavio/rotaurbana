document.getElementById("loginForm").addEventListener("submit", async function(e) {
    e.preventDefault();

    const btn = this.querySelector("button[type='submit']");
    const originalText = btn.textContent;
    btn.disabled = true;
    btn.textContent = "Entrando...";

    const data = {
        email: document.querySelector("input[name='email']").value.toLowerCase().trim(),
        password: document.querySelector("input[name='password']").value
    };

    try {
        const response = await fetch("/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            const result = await response.json();

            const expires = new Date(Date.now() + 2 * 60 * 60 * 1000).toUTCString();
            document.cookie = "token=" + result.token + "; path=/; expires=" + expires;
            document.cookie = "userId=" + result.userId + "; path=/; expires=" + expires;
            if (result.driverId) document.cookie = "driverId=" + result.driverId + "; path=/; expires=" + expires;
            document.cookie = "role=" + result.role + "; path=/; expires=" + expires;

            if (result.role === "ADMIN") {
                window.location.href = "/admin/routes";
            } else if (result.role === "DRIVER") {
                window.location.href = "/driver";
            } else {
                window.location.href = "/passenger";
            }
        } else {
            const text = await response.text();
            alert(text || "E-mail ou senha inválidos");
        }
    } catch (err) {
        alert("Erro de conexão: " + err.message);
    } finally {
        btn.disabled = false;
        btn.textContent = originalText;
    }
});
