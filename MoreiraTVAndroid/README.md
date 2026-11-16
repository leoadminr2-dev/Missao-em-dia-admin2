# Moreira TV Android — 1.0 (beta)

Aplicativo Android (APK), sem dependência de computador para uso, compatível com o protocolo Xtream Codes (`player_api.php`).

- Informar servidor, usuário e senha diretamente no aparelho.
- Consultar TV ao vivo, filmes, séries e episódios.
- Reprodução com Media3/ExoPlayer, HLS `.m3u8` e fallback MPEG-TS `.ts`.
- O botão 'OUTRO PLAYER' permite abrir o stream em um reprodutor instalado no celular.
- Não salva as credenciais em disco e não inclui nenhuma lista de canais.
- A URL padrão é http://aurinhatv.com; não há credenciais incluídas no código.

**Avisos de segurança**: O servidor informado usa HTTP sem TLS, o que transmite os dados de login sem criptografia em redes intermediárias. Utilize-o somente se o titular tiver autorização de uso; não compartilhe o APK com credenciais embutidas (elas não estão embutidas nesta versão).

**Como compilar:** `gradle :app:assembleDebug` usando Android SDK 35, JDK 17 e Gradle 8.11.1.

**Teste no aparelho:** instalar o APK, conceder instalação de fonte externa caso solicitada, inserir credenciais, tocar CONECTAR. Uma conexão recusada não significa necessariamente credenciais inválidas; pode ser restrição de rede ou host.
