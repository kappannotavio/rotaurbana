document.getElementById("loginForm").addEventListener("submit", async function(e) {
    e.preventDefault();

    const data = {
        email: document.querySelector("input[name='email']").value,
        password: document.querySelector("input[name='password']").value
    };

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
            window.location.href = "/admin";
        } else if (result.role === "DRIVER") {
            window.location.href = "/driver";
        } else {
            window.location.href = "/passenger";
        }
    } else {
        alert("Login inv\u00e1lido");
    }
});
