# Finances

## Estrutura

- `web/` - aplicação Nuxt (todo o código web: `app/`, `i18n/`, config e assets).
- `app/` - app Android Kotlin com Jetpack Compose, consumindo os mesmos endpoints do web.
- `Dockerfile`, `docker-compose.yaml`, `docker-compose.prod.yml` e `docker/` — build e publicação em `/app/finances/`.
- `.github/` — CI e publicação da imagem `finances-ui`.

## Desenvolvimento web

```bash
cd web
pnpm install
pnpm dev
```

## App Android

```bash
cd app
./gradlew :app:assembleDebug
```

Configuração em `app/local.properties` (não versionado) ou variáveis de ambiente:

- `ehs.finances.apiBaseUrl` / `EHS_FINANCES_API_BASE_URL` — URL base da API (padrão `http://10.0.2.2/`, que é o host visto pelo emulador).
- `ehs.finances.googleClientId` / `EHS_FINANCES_GOOGLE_CLIENT_ID` — Client ID OAuth (Web) do Google. Vazio oculta o botão de login com Google.

O app usa os mesmos endpoints do web (`users`, `finances`): contas, preferências, transações,
categorias, recorrências, metas mensais, projeção de caixa e importação de extrato CSV/OFX.

## Docker / deploy

O web roda em um container nginx próprio e é publicado pelo nginx dos microservices em **`/app/finances/`**, sem publicar porta no host em produção. O compose local (`docker-compose.yaml`) permite build local; o compose de produção consome a imagem publicada no GHCR:

```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

- A imagem do GitHub Actions é `ghcr.io/eduhsilva/finances-ui`, com tags `latest` e `<sha>`.
- Para fixar uma versão, defina `IMAGE_TAG=<sha>`. Para substituir o endereço completo, defina `FINANCES_UI_IMAGE`.
- A rede externa padrão em produção é `microservices_backend`, alinhada ao compose de produção de `study` e `admin`. Se necessário, defina `EHS_MICROSERVICES_NETWORK` com o nome existente da rede Docker.
- O serviço publica internamente a porta 80 e usa o alias `finances-ui`; não há porta host publicada. A rota `/app/finances/` do nginx dos microservices deve encaminhar para `finances-ui:80`.
- O build fixa o base path `/app/finances/` e usa API same-origin (`/users` e `/finances`) via gateway.
- Login Google: o `NUXT_PUBLIC_GOOGLE_CLIENT_ID` é compilado no bundle estático, então precisa existir no build (local: `FINANCES_GOOGLE_CLIENT_ID=... docker compose build`; CI: var de repositório `FINANCES_GOOGLE_CLIENT_ID`). Vazio = botão oculto.
- Não são necessários segredos no compose do front.

Rollback: execute novamente `pull` e `up -d` com `IMAGE_TAG` definido para a tag SHA anterior.

## Documentação do produto

O plano de evolução do web, do app Android e dos contratos necessários nos microservices está em [`todo.md`](todo.md).
