# MOREIRA PLAY 3.0 — Android (base única para atualizações)

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

## Melhorias da versão 3.0.0
- Tela cheia preservada em todo o catálogo, com escolha entre preencher a tela e mostrar o quadro sem cortes.
- Troca rápida entre canais da lista filtrada por gesto horizontal, ou pelas opções de toque prolongado (botões de canal do controle quando disponíveis). O player limita a fila local de troca para não exceder o limite de transferência entre telas do Android.
- Reconexão em falhas temporárias até duas vezes e alternativa HLS/TS para TV ao vivo.
- Abas locais **Todos**, **Favoritos** e **Recentes** por tipo de conteúdo e conta, sem enviar histórico a serviços externos.
- Retomada de filmes e episódios pelo ponto salvo na memória local.
- Guia de programação ao pressionar um canal, desde que o serviço disponibilize `get_short_epg`; na ausência de dados, o aplicativo mostra uma mensagem amigável.
- Cache temporário de catálogo por tipo por 5 minutos, botão **Atualizar** e thumbnails com dimensões limitadas para reduzir uso de memória.
- Menu de **Verificar atualizações** preservado; mesmo identificador `br.com.moreiraplay.app` e certificado privado original.
- Esta etapa é exclusiva do **Android**. A adaptação ao webOS da LG será trabalhada depois, como um pacote IPK nativo do sistema, não APK.
