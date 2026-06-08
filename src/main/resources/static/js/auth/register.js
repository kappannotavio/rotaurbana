document.getElementById("registerForm").addEventListener("submit", async function(e) {
    e.preventDefault();

    const password = document.getElementById("password").value;
    const confirm = document.getElementById("confirmPassword").value;
    const errorEl = document.getElementById("errorMsg");

    if (password !== confirm) {
        errorEl.textContent = "Senhas não conferem";
        errorEl.style.display = "";
        return;
    }

    errorEl.style.display = "none";

    const btn = document.getElementById("btnRegister");
    btn.disabled = true;
    btn.textContent = "Cadastrando...";

    try {
        let userImageUrl = "padrao";
        const fileInput = document.getElementById("registerImage");
        if (fileInput && fileInput.files.length > 0) {
            const formData = new FormData();
            formData.append("file", fileInput.files[0]);
            const uploadRes = await fetch("/api/upload/image", {
                method: "POST",
                body: formData
            });
            if (uploadRes.ok) {
                const uploadData = await uploadRes.json();
                userImageUrl = uploadData.imageUrl;
            }
        }

        const res = await fetch("/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                fullName: document.getElementById("fullName").value,
                adress: document.getElementById("adress").value,
                city: document.getElementById("city").value,
                email: document.getElementById("email").value.toLowerCase().trim(),
                password: password,
                confirmPassword: confirm,
                birthDate: document.getElementById("birthDate").value,
                userImageUrl: userImageUrl
            })
        });

        if (res.ok) {
            window.location.href = "/auth/login";
        } else {
            const text = await res.text();
            errorEl.textContent = text ? "Erro: " + text : "E-mail já cadastrado";
            errorEl.style.display = "";
        }
    } catch (err) {
        errorEl.textContent = "Erro de conexão";
        errorEl.style.display = "";
    } finally {
        btn.disabled = false;
        btn.textContent = "CRIAR CONTA";
    }
});

document.getElementById("registerImage").addEventListener("change", function() {
    const file = this.files[0];
    if (!file) return;
    const preview = document.getElementById("registerImagePreview");
    preview.src = URL.createObjectURL(file);
    preview.style.display = "block";
});
