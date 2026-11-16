# Moreira TV Premium — Android v1.1 (beta)

Player de TV personalizado para uso privado no Android, com catálogo TV ao vivo, filmes, séries e episódios. A transmissão depende da conta do serviço contratada/licenciada pelo usuário.

## Como usar
1. Instale `Moreira-TV-Premium.apk` no Android.
2. Na primeira abertura, preencha o endereço de conexão, código e chave pessoal da conta.
3. Toque **ENTRAR NO MOREIRA TV**. Se a autenticação for válida, a programação abrirá.
4. Nas próximas aberturas, o aplicativo reconectará automaticamente. Não será necessário preencher login novamente.
5. Use o ícone **⚙** para configurar outra conta ou apagar o acesso salvo deste aparelho.

## Segurança e compatibilidade
- Não existem senhas compartilhadas ou credenciais de serviços no código nem no APK.
- Os dados digitados ficam criptografados com AES-GCM, chave protegida por Android Keystore; backup Android está desativado.
- A autenticação do servidor continua ocorrendo internamente a cada conexão. O login visual é ocultado após a primeira ativação; não se trata de burlar autenticação do fornecedor.
- O servidor inicialmente configurado usa HTTP sem TLS: ao trafegar na rede, credenciais e streams continuam sujeitos à insegurança de HTTP. Use HTTPS se o provedor disponibilizar.
- Instalação paralela: o applicationId é `br.com.moreira.tv.premium`, para não substituir nem exigir desinstalação da versão 1.0 funcional.
- Mesmo que a versão anterior esteja conectada, por não haver armazenamento de credenciais na v1.0, é necessário preenchê-las **uma vez** na v1.1.
- O player usa Media3/ExoPlayer, HLS `.m3u8` e alternativa MPEG-TS `.ts` quando apropriado.

## Compilação
JDK 17, Android SDK 35, Gradle 8.11.1; `gradle :app:assembleDebug`. O GitHub Actions publica a build em `public-download/Moreira-TV-Premium.apk` quando bem-sucedida. É uma versão de teste assinada com certificado debug da automação, não um release estável. Verifique o resultado real antes de instalar.
