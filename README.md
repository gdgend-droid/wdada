# Minecraft 26.2 Player Radar (Fabric client mod + Render website)

Fabric client mod -> Render website (radar) -> your phone.

## 1. Deploy the website on Render
1. Push this folder to GitHub.
2. Render -> New -> Blueprint -> pick the repo (reads render.yaml).
   (Or New -> Web Service, Root Directory `web`, Build `npm install`, Start `npm start`.)
3. Environment tab: copy `RADAR_API_KEY`. Optionally set `VIEW_PASSWORD` to lock the page.
4. Open `https://<your-service>.onrender.com` on your phone (Add to Home Screen for an app feel).

## 2. Get the mod .jar (no Java install needed)
GitHub repo -> Actions tab -> "Build PlayerRadar mod" -> latest run -> download the
`PlayerRadar-mod` artifact (zip containing playerradar-1.0.0.jar).

## 3. Install
Needs Minecraft 26.2, Fabric Loader 0.19.3+, Fabric API, Java 25.
Drop the jar in `.minecraft/mods/`, launch once, then edit `config/playerradar.properties`
(url + api-key) and restart the game.

## Notes
- The mod only sees players your client has loaded (roughly your render/entity range),
  so it's not a whole-server radar. Your own position is included.
- Many servers ban radar/tracker mods. Only use it where allowed.
- Render free plan sleeps after ~15 min idle; first load may take ~30s.
- If Gradle can't resolve a version, check fabricmc.net/develop for the current
  Loom / Fabric API for 26.2 and edit `mod/gradle.properties`.
