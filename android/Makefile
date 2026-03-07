.PHONY: monitor stop log clean

monitor:
	@bash uber_monitor.sh

stop:
	@pkill -f uber_monitor.sh 2>/dev/null && echo "Monitor detenido" || echo "No estaba corriendo"

log:
	@cat uber_viajes.log

clean:
	@rm -f uber_viajes.log && echo "Log limpiado"
