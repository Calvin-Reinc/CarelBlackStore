<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="<%= request.getContextPath() %>/">
    <title>flying Jvck</title>

    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet"
          integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"
          crossorigin="anonymous">

    <!-- Monospace web font -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Space+Mono:wght@400;700&display=swap" rel="stylesheet">

    <style>
        @font-face {
            font-family: "Super Squad";
            src: url("assets/fonts/SuperSquad.woff2") format("woff2"),
                 url("assets/fonts/SuperSquad.ttf") format("truetype"),
                 url("assets/fonts/SuperSquad.otf") format("opentype");
            font-weight: normal;
            font-style: normal;
            font-display: swap;
        }

        html, body {
            background-color: #ffffff;
            color: #111111;
            font-family: "Space Mono", ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            margin: 0;
            padding: 0;
        }

        .error-shell {
            min-height: 100vh;
            min-height: 100dvh;
        }

        .brand-logo {
            width: clamp(82px, 16vw, 130px);
            height: auto;
            background-color: transparent;
            display: block;
            margin-inline: auto;
            transition: transform 0.25s ease;
        }

        a:hover .brand-logo {
            transform: scale(1.03);
        }

        .brand-title {
            position: relative;
            display: inline-block;
            padding-inline: 0.15em;
            font-family: "Super Squad", Impact, "Arial Black", sans-serif;
            font-size: clamp(2.6rem, 9vw, 5.4rem);
            line-height: 1.05;
            letter-spacing: 0.03em;
            color: #111111;
            word-break: break-word;
            cursor: default;
            user-select: none;
            transition: color 0.35s ease;
        }

        /* Zoomed artwork masked cleanly INSIDE the whole letters */
        .brand-title::after {
            content: attr(data-text);
            position: absolute;
            inset: 0;
            padding: inherit;
            background-image:
                url("assets/img/title-art.jpg?v=2"),
                url("assets/img/title-art.png?v=2"),
                linear-gradient(
                    135deg,
                    #ff2a6d 0%,
                    #ff7b00 20%,
                    #ffd400 40%,
                    #00c853 60%,
                    #00b0ff 80%,
                    #7c4dff 100%
                );
            background-size: 220% auto;
            background-position: 50% 50%;
            background-repeat: no-repeat;
            -webkit-background-clip: text;
            background-clip: text;
            color: transparent;
            -webkit-text-fill-color: transparent;
            opacity: 0;
            transition: opacity 0.4s ease, background-size 0.6s ease, background-position 0.6s ease;
            pointer-events: none;
        }

        .brand-title:hover,
        .brand-title:focus-visible,
        .brand-title.revealed {
            color: transparent;
            outline: none;
        }

        .brand-title:hover::after,
        .brand-title:focus-visible::after,
        .brand-title.revealed::after {
            opacity: 1;
            background-size: 250% auto;
            background-position: 52% 48%;
        }

        @media (max-width: 575.98px) {
            .brand-title::after {
                background-size: 300% auto;
            }
            .brand-title:hover::after,
            .brand-title:focus-visible::after,
            .brand-title.revealed::after {
                background-size: 330% auto;
            }
        }

        .live-clock {
            font-size: clamp(0.7rem, 1.8vw, 0.82rem);
            letter-spacing: 0.08em;
            color: #6c757d;
            text-transform: uppercase;
        }
    </style>
</head>
<body class="bg-white">

    <main class="error-shell d-flex flex-column justify-content-center align-items-center text-center px-3">
        <!-- Logo -> Store Home (index.html) -->
        <a href="index.html" class="d-inline-block mb-2 mb-md-3" aria-label="flying Jvck Home">
            <img src="assets/img/Artwork.png"
                 onerror="if (!this.dataset.fallback) { this.dataset.fallback = '1'; this.src = 'assets/img/logo.png'; }"
                 alt="flying Jvck Logo"
                 class="brand-logo img-fluid">
        </a>

        <!-- Title -->
        <h1 class="brand-title mb-2"
            id="brandTitle"
            data-text="flying Jvck"
            tabindex="0"
            aria-label="flying Jvck">flying Jvck</h1>

        <!-- Live Pretoria Time & Birthplace (no seconds) -->
        <p id="ptaClock" class="live-clock mb-0">PTA // --/--/---- --:--</p>
    </main>

    <script>
        function updatePtaClock() {
            const now = new Date();
            const datePart = new Intl.DateTimeFormat('en-GB', {
                timeZone: 'Africa/Johannesburg',
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
            }).format(now);

            const timePart = new Intl.DateTimeFormat('en-GB', {
                timeZone: 'Africa/Johannesburg',
                hour: '2-digit',
                minute: '2-digit',
                hour12: true
            }).format(now);

            document.getElementById('ptaClock').textContent = 'PTA // ' + datePart + ' ' + timePart + ' SAST';
        }

        updatePtaClock();
        setInterval(updatePtaClock, 5000);

        const brandTitle = document.getElementById('brandTitle');
        brandTitle.addEventListener('click', () => brandTitle.classList.toggle('revealed'));
        brandTitle.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                brandTitle.classList.toggle('revealed');
            }
        });
    </script>

</body>
</html>