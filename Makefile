# Atalhos do projeto final (eleicoes-kmp). Tudo é feito por scripts/rodar-app.sh, que funciona em
# qualquer Linux (nativo ou WSL 2) e detecta sozinho o JDK 21 e o Android SDK.
#
#   make                 → ajuda
#   make setup           → instala JDK 21, Android SDK, emulador e o resto (SETUP_ARGS='--yes --no-emulator')
#   make check           → confere o ambiente
#   make install         → compila, instala e abre o app (liga o emulador se preciso)
#   make test            → todos os testes
#   make test-shared ARGS='--tests "*PartidosTest*"'
#   make phone P_PAR=192.168.0.10:37123 COD=123456 P_CON=192.168.0.10:41234
#   make shot NOME=detalhe

SCRIPT := scripts/rodar-app.sh
ARGS   ?=

.DEFAULT_GOAL := help
SETUP_ARGS ?=

.PHONY: help setup check emulator stop phone rotate logs install open shot build test test-shared test-app ci reports clean

help:        ## Mostra a ajuda
	@$(SCRIPT) help

setup:       ## Instala o que faltar (JDK 21, Android SDK, emulador, AVD, libs, local.properties)
	@$(SCRIPT) setup $(SETUP_ARGS)

check:       ## Confere JDK 21, Android SDK, chave da API, KVM, janelas e bibliotecas do emulador
	@$(SCRIPT) check

emulator:    ## Liga o emulador e espera o Android iniciar
	@$(SCRIPT) emulator

stop:        ## Desliga o emulador
	@$(SCRIPT) stop

phone:       ## Conecta o celular por Wi-Fi (P_PAR=IP:porta COD=código P_CON=IP:porta)
	@test -n "$(P_PAR)" -a -n "$(COD)" -a -n "$(P_CON)" || { echo "uso: make phone P_PAR=IP:porta COD=123456 P_CON=IP:porta"; exit 1; }
	@$(SCRIPT) phone "$(P_PAR)" "$(COD)" "$(P_CON)"

rotate:      ## Gira a tela do emulador 90°
	@$(SCRIPT) rotate

logs:        ## Últimas linhas do log do emulador (N=40)
	@$(SCRIPT) logs $(or $(N),40)

install:     ## Compila, instala e abre o app
	@$(SCRIPT) install

open:        ## Abre o app já instalado
	@$(SCRIPT) open

shot:        ## Print da tela em scripts/prints/NOME.png
	@$(SCRIPT) shot $(or $(NOME),print)

build:       ## Compila o APK de debug
	@$(SCRIPT) build $(ARGS)

test:        ## Todos os testes (shared + androidApp)
	@$(SCRIPT) test $(ARGS)

test-shared: ## Testes do domínio e dos dados
	@$(SCRIPT) test-shared $(ARGS)

test-app:    ## Testes dos ViewModels
	@$(SCRIPT) test-app $(ARGS)

ci:          ## O mesmo comando do CI (build + todos os testes)
	@$(SCRIPT) ci $(ARGS)

reports:     ## Abre os relatórios HTML dos testes
	@$(SCRIPT) reports

clean:       ## Limpa os builds
	@$(SCRIPT) clean
