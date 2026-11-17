# MOREIRA PLAY — Android (base única para atualizações)

Este diretório no branch `moreira-play-canonical` contém o app Android oficial para atualizações futuras. **Não trocar applicationId** `br.com.moreiraplay.app` em lançamentos posteriores.

## Fluxo de atualização
- Identidade visual: MOREIRA PLAY.
- Novo usuário instala uma vez o APK assinado e distribuído em `https://moreira-play-updates.pages.dev/`.
- O menu de configurações do app inclui **Verificar atualizações**.
- Na abertura, o app também consulta `https://moreira-play-updates.pages.dev/manifest.json`.
- Se existir versão nova, o app solicita confirmação para baixar e instala o APK por cima, utilizando `FileProvider` e o instalador oficial do Android.
- Antes de instalar, exige `app_slug` correspondente, versionCode maior, HTTPS do host autorizado, SHA-256 correto, `applicationId` igual e assinatura digital compatível.
- O usuário confirma no Android as permissões obrigatórias; não há instalação silenciosa.
- A chave de assinatura de teste permanente fica apenas no repositório **privado** `leoadminr2-dev/missao-em-dia-admin`, junto com o workflow `moreira-play-stable-release.yml`. Nunca mover a chave para o repositório público.
- O workflow privado compila, valida assinatura, publica o APK/manifesto em novo projeto Cloudflare Pages sem substituir a Central.

## Migração a partir das versões 1.0, 1.1, 1.2, 1.3
Os APKs anteriores foram distribuídos com `applicationId` diferentes e chaves de debug temporárias. **O Android não permite transformar esses três apps em um por atualização.** A mudança para `br.com.moreiraplay.app` exige uma última instalação independente. Só depois de verificar o novo app, o usuário pode remover os ícones antigos.

## Próximos lançamentos
Aumentar `versionCode` e `versionName`; manter `applicationId` e a chave de assinatura, construir no workflow privado, conferir que a publicação HTTPS recebeu o APK correto e o manifesto de versão. Nunca sobrescrever os binários das versões antigas antes de validar a nova. A Central e os outros aplicativos não são alterados.
