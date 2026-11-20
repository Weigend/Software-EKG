ASPROF         := $(HOME)/async-profiler/bin/asprof
RESULTS_DIR    := $(HOME)/async-profiler/results
EVENT          ?= wall
DURATION       ?= 30
PID            ?= $(shell jps -l 2>/dev/null | grep 'EkgCdiApplication' | awk '{print $$1}')
TIMESTAMP      := $(shell date +%Y%m%d-%H%M%S)
FLAMEGRAPH     := $(RESULTS_DIR)/$(EVENT)-$(TIMESTAMP).html

.PHONY: help startprofiling stopprofiling flamegraph status list-events pid check-pid check-asprof

help: ## Show this help
	@echo ""
	@echo "Software-EKG Profiling Tools"
	@echo "============================"
	@echo ""
	@echo "Targets:"
	@echo "  make startprofiling    Start wall-clock profiling (attach to AWB)"
	@echo "  make stopprofiling     Stop profiling and generate flamegraph HTML"
	@echo "  make flamegraph        One-shot: profile for DURATION seconds, then generate flamegraph"
	@echo "  make status            Show current profiler status"
	@echo "  make list-events       List available profiling events"
	@echo "  make pid               Show AWB process ID"
	@echo "  make help              Show this help"
	@echo ""
	@echo "Variables (override with VAR=value):"
	@echo "  EVENT=$(EVENT)            Profiling event (wall, alloc, lock, cpu, itimer)"
	@echo "  DURATION=$(DURATION)           Duration in seconds (for 'make flamegraph')"
	@echo "  PID=$(PID)          AWB process ID (auto-detected)"
	@echo ""
	@echo "Examples:"
	@echo "  make startprofiling                  Start wall-clock profiling"
	@echo "  make stopprofiling                   Stop and write flamegraph"
	@echo "  make flamegraph DURATION=60          One-shot 60s wall-clock flamegraph"
	@echo "  make flamegraph EVENT=alloc          One-shot allocation flamegraph"
	@echo "  make startprofiling EVENT=lock       Start lock-contention profiling"
	@echo ""
	@echo "Results are written to: $(RESULTS_DIR)/"
	@echo ""

check-asprof:
	@test -x $(ASPROF) || { echo "ERROR: asprof not found at $(ASPROF)"; exit 1; }

check-pid: check-asprof
	@test -n "$(PID)" || { echo "ERROR: AWB process not found. Is EkgCdiApplication running?"; echo "       You can set it manually: make <target> PID=<pid>"; exit 1; }
	@echo "AWB PID: $(PID)"

pid: ## Show AWB process ID
	@if [ -n "$(PID)" ]; then \
		echo "AWB PID: $(PID)"; \
		echo "Command: $$(ps -p $(PID) -o args= 2>/dev/null)"; \
	else \
		echo "AWB process not found. Is EkgCdiApplication running?"; \
		echo "All Java processes:"; \
		jps -l 2>/dev/null; \
	fi

startprofiling: check-pid ## Start profiling (use EVENT=wall|alloc|lock|cpu)
	@echo "Starting $(EVENT) profiling on PID $(PID)..."
	$(ASPROF) start -e $(EVENT) $(PID)
	@echo ""
	@echo "Profiling is running. Now perform the action you want to measure."
	@echo "When done, run: make stopprofiling"

stopprofiling: check-pid ## Stop profiling and generate flamegraph
	@mkdir -p $(RESULTS_DIR)
	@echo "Stopping profiler and writing flamegraph..."
	$(ASPROF) stop -f $(FLAMEGRAPH) $(PID)
	@echo ""
	@echo "Flamegraph written to: $(FLAMEGRAPH)"
	@echo "Open with: xdg-open $(FLAMEGRAPH)"

flamegraph: check-pid ## One-shot: profile for DURATION seconds, then generate flamegraph
	@mkdir -p $(RESULTS_DIR)
	@echo "Profiling $(EVENT) for $(DURATION) seconds on PID $(PID)..."
	$(ASPROF) -e $(EVENT) -d $(DURATION) -f $(FLAMEGRAPH) $(PID)
	@echo ""
	@echo "Flamegraph written to: $(FLAMEGRAPH)"
	@echo "Open with: xdg-open $(FLAMEGRAPH)"

status: check-asprof ## Show current profiler status
	@if [ -n "$(PID)" ]; then \
		$(ASPROF) status $(PID) 2>&1; \
	else \
		echo "AWB process not found."; \
	fi

list-events: check-pid ## List available profiling events
	$(ASPROF) list $(PID)
