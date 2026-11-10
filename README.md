# ROPC Desktop — simulador de PC para Android

Aplicativo Android nativo em Kotlin que apresenta uma interface de desktop simulada, otimizada para uso com teclado e mouse físicos.

## O que está implementado

- Área de trabalho com atalhos.
- Menu Iniciar e barra de tarefas.
- Janelas que podem ser movidas, minimizadas, maximizadas e fechadas.
- Aplicativos demonstrativos: Este PC, Arquivos, Navegador, Terminal e Configurações.
- Terminal local de demonstração com os comandos `help`, `about` e `clear`.
- Atalhos básicos: tecla Meta abre o menu Iniciar, Esc fecha o menu e F4 fecha a janela ativa.
- Detecção das arquiteturas Android suportadas e dos dispositivos de entrada físicos.
- Compilação Android para ABIs `arm64-v8a` e `x86_64`.

## Limitações importantes

Este projeto é um **simulador de interface desktop**. Não é Windows, não emula um PC x86 e não executa programas Windows `.exe`. As janelas Arquivos e Navegador são demonstrativas e ainda não estão ligadas ao armazenamento ou à internet.

A configuração `abiFilters` apenas define quais arquiteturas Android entram no APK; ela não cria emulação de CPU. Este projeto também não inclui Wine/Box64, não executa o cliente Roblox para Windows, não falsifica a identidade do aparelho e não tenta contornar mecanismos anti-cheat. Portanto, não há garantia de que o Roblox o reconheça como PC.

## Compilar

Requisitos: JDK 17 e Gradle 8.9.

```bash
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

1. Abra **Actions** no repositório.
2. Selecione **Build Android APK**.
3. Aguarde a execução terminar.
4. Baixe o artefato `roblox-pc-android-debug` quando a compilação for concluída com sucesso.
