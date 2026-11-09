# Roblox PC Mode Android — scaffold inicial

Protótipo Android nativo em Kotlin, configurado para compilar para `arm64-v8a` e
`x86_64`, detectar teclado/mouse físicos e gerar APK pelo GitHub Actions.

## O que esta versão faz

- Compila um APK Android.
- Inclui as ABIs Android `arm64-v8a` e `x86_64`.
- Detecta teclado e mouse USB/Bluetooth conectados ao Android.
- Não desenha botões virtuais.
- Não usa streaming.

## O que esta versão NÃO faz

Este repositório ainda não é um emulador Windows e não executa o Roblox para PC.
Os filtros ABI definem as arquiteturas Android do APK; eles não emulam processadores
Windows nem fornecem uma camada de compatibilidade.

Para executar aplicativos Windows no Android seria necessário integrar e manter
uma camada de compatibilidade (por exemplo, Wine mais tradução de CPU e gráficos
adequada para cada ABI). Isso exige componentes nativos específicos, licenças e
testes de desempenho. Não há garantia de que o cliente Windows do Roblox funcione
nesse ambiente ou seja aceito pelos mecanismos de segurança do Roblox. O projeto
não falsifica a identidade do dispositivo, modifica o cliente ou contorna anti-cheat.

## Gerar localmente

Requisitos: JDK 17 e Gradle 8.9.

```bash
gradle :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Gerar pelo GitHub Actions

1. Crie um repositório no GitHub.
2. Envie os arquivos deste projeto para a branch `main`.
3. Abra **Actions → Build Android APK**.
4. Execute com **Run workflow** ou faça push.
5. Baixe o artefato `roblox-pc-android-debug` na execução concluída.

## Próxima etapa técnica

Antes de integrar um runtime Windows, validar:
- ABI real do aparelho (`arm64-v8a` ou `x86_64`);
- disponibilidade de Vulkan/OpenGL e extensões exigidas;
- suporte a teclado e mouse no runtime escolhido;
- compatibilidade com os termos e requisitos técnicos do Roblox.
