#!/bin/bash
DEVICE="192.168.0.144:45955"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_FILE="$SCRIPT_DIR/uber_viajes.log"
NBSP=$(printf '\xc2\xa0')

echo "" >> "$LOG_FILE"
echo "=== SESION $(date) ===" >> "$LOG_FILE"

while true; do
    XML=$(adb -s "$DEVICE" exec-out uiautomator dump /dev/tty 2>/dev/null)
    [ -z "$XML" ] && sleep 1 && continue

    T=$(date '+%H:%M:%S')
    TEXTS=$(echo "$XML" | sed 's/>/>\n/g' | grep -o 'text="[^"]*"' | sed 's/text="//;s/"$//' | sed "s/$NBSP/ /g" | grep -v '^$')

    HAS_TRIP=$(echo "$TEXTS" | grep -cE '^Aceptar$|^Viaje disponible$')

    if [ "$HAS_TRIP" -gt 0 ]; then
        PRICE=$(echo "$TEXTS" | grep -E '^\$ ' | grep -v '^\+' | head -1)
        BONUS=$(echo "$TEXTS" | grep 'incluido' | head -1)
        IDENTITY=$(echo "$TEXTS" | grep -iE 'verificad|DNI' | head -1)
        RATING=$(echo "$TEXTS" | grep -E '^[0-9],[0-9]' | head -1)
        PICKUP_DIST=$(echo "$TEXTS" | grep -E '^A [0-9]' | head -1)
        TRIP_INFO=$(echo "$TEXTS" | grep -E '^Viaje:' | head -1)
        TYPE=$(echo "$TEXTS" | sed -n '1p')
        LINE2=$(echo "$TEXTS" | sed -n '2p')
        SUBTYPE=""
        echo "$LINE2" | grep -qvE '^\$' && SUBTYPE="$LINE2"

        PICKUP_LINE=$(echo "$TEXTS" | grep -nE '^A [0-9]' | head -1 | cut -d: -f1)
        PICKUP_ADDR=""
        [ -n "$PICKUP_LINE" ] && PICKUP_ADDR=$(echo "$TEXTS" | sed -n "$((PICKUP_LINE + 1))p")

        TRIP_LINE=$(echo "$TEXTS" | grep -nE '^Viaje:' | head -1 | cut -d: -f1)
        DEST=""
        [ -n "$TRIP_LINE" ] && DEST=$(echo "$TEXTS" | sed -n "$((TRIP_LINE + 1))p")

        # Log al archivo
        echo "[$T] $TYPE $SUBTYPE | $PRICE | $BONUS | $IDENTITY $RATING | $PICKUP_DIST - $PICKUP_ADDR | $TRIP_INFO | Destino: $DEST" >> "$LOG_FILE"

        # Mostrar formateado
        printf "\033[1;32m"
        echo "=========================================="
        printf "  VIAJE  %-10s  %s\n" "$PRICE" "$T"
        echo "=========================================="
        printf "\033[0m"
        printf "  %-12s \033[1;36m%s %s\033[0m\n" "Tipo:" "$TYPE" "$SUBTYPE"
        printf "  %-12s \033[1;33m%s\033[0m\n" "Precio:" "$PRICE"
        printf "  %-12s %s\n" "Bonus:" "${BONUS:--}"
        printf "  %-12s %s %s\n" "Pasajero:" "$IDENTITY" "$RATING"
        printf "  %-12s %s\n" "Recogida:" "$PICKUP_DIST"
        printf "  %-12s %s\n" "" "$PICKUP_ADDR"
        printf "  %-12s %s\n" "Duracion:" "$TRIP_INFO"
        printf "  %-12s \033[1;35m%s\033[0m\n" "Destino:" "$DEST"
        echo "------------------------------------------"
    else
        printf "[\033[90m%s\033[0m] Buscando viajes...\n" "$T"
    fi

    sleep 1
done
