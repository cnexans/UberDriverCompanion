package com.carlos.uberanalyzer.parser

import org.junit.Assert.*
import org.junit.Test

class TripParserTest {

    // === Existing tests ===

    @Test
    fun `parse basic Moto trip`() {
        val texts = listOf(
            "Moto",
            "$ 1.500",
            "A 3 min (1.2 km)",
            "Av. Corrientes 1234",
            "Viaje: 10 min (5.0 km)",
            "Palermo",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("Moto", trip!!.type)
        assertEquals(1500.0, trip.price, 0.01)
        assertEquals(3, trip.pickupMinutes)
        assertEquals(1.2, trip.pickupKm, 0.01)
        assertEquals(10, trip.tripMinutes)
        assertEquals(5.0, trip.tripKm, 0.01)
        assertEquals("Palermo", trip.destination)
    }

    @Test
    fun `parse Auto trip with bonus`() {
        val texts = listOf(
            "Auto",
            "$ 2.300",
            "+$ 500 incluido",
            "A 5 min (2.0 km)",
            "Calle San Martín 500",
            "Viaje: 15 min (8.5 km)",
            "Belgrano",
            "4,85 (120)",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("Auto", trip!!.type)
        assertEquals(2300.0, trip.price, 0.01)
        assertEquals(500.0, trip.bonus!!, 0.01)
        assertEquals(5, trip.pickupMinutes)
        assertEquals(2.0, trip.pickupKm, 0.01)
        assertEquals(15, trip.tripMinutes)
        assertEquals(8.5, trip.tripKm, 0.01)
        assertEquals("4,85 (120)", trip.passengerRating)
    }

    @Test
    fun `parse Exclusivo trip`() {
        val texts = listOf(
            "Exclusivo",
            "$ 5.800",
            "A 2 min (0.8 km)",
            "Av. Libertador 4000",
            "Viaje: 20 min (12.0 km)",
            "Ezeiza",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("Exclusivo", trip!!.type)
        assertEquals(5800.0, trip.price, 0.01)
        assertEquals(12.0, trip.tripKm, 0.01)
    }

    @Test
    fun `parse trip with identity verification`() {
        val texts = listOf(
            "Auto",
            "$ 1.800",
            "A 4 min (1.5 km)",
            "Calle Rivadavia",
            "Viaje: 8 min (3.2 km)",
            "Caballito",
            "DNI verificado",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("DNI verificado", trip!!.identity)
    }

    @Test
    fun `returns null when no pickup or trip info`() {
        val texts = listOf(
            "Moto",
            "$ 1.500",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNull(trip)
    }

    @Test
    fun `returns null when price is zero`() {
        val texts = listOf(
            "$ 0",
            "A 3 min (1.0 km)",
            "Viaje: 5 min (2.0 km)"
        )
        val trip = TripParser.parse(texts)
        assertNull(trip)
    }

    @Test
    fun `returns null when no price`() {
        val texts = listOf(
            "Moto",
            "A 3 min (1.0 km)",
            "Viaje: 5 min (2.0 km)"
        )
        val trip = TripParser.parse(texts)
        assertNull(trip)
    }

    @Test
    fun `parse trip with comma price format`() {
        val texts = listOf(
            "Auto",
            "$ 3.250,50",
            "A 6 min (3.0 km)",
            "Florida 100",
            "Viaje: 12 min (6.5 km)",
            "Retiro",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(3250.50, trip!!.price, 0.01)
    }

    @Test
    fun `pesosPorKm calculated correctly`() {
        val texts = listOf(
            "Moto",
            "$ 2.000",
            "A 2 min (1.0 km)",
            "Calle Test",
            "Viaje: 10 min (4.0 km)",
            "Destino",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(400.0, trip!!.pesosPorKm, 0.01)
    }

    @Test
    fun `pesosPorMin calculated correctly`() {
        val texts = listOf(
            "Moto",
            "$ 1.200",
            "A 3 min (1.0 km)",
            "Calle Test",
            "Viaje: 9 min (4.0 km)",
            "Destino",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(100.0, trip!!.pesosPorMin, 0.01)
    }

    @Test
    fun `pctDistancia calculated correctly`() {
        val texts = listOf(
            "Moto",
            "$ 1.000",
            "A 2 min (2.0 km)",
            "Calle Test",
            "Viaje: 5 min (8.0 km)",
            "Destino",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(80.0, trip!!.pctDistancia, 0.01)
    }

    @Test
    fun `tripKey is consistent for same trip`() {
        val texts = listOf(
            "Moto",
            "$ 1.500",
            "A 3 min (1.2 km)",
            "Calle Test",
            "Viaje: 10 min (5.0 km)",
            "Destino",
            "Aceptar"
        )
        val trip1 = TripParser.parse(texts)
        val trip2 = TripParser.parse(texts)
        assertEquals(trip1!!.tripKey, trip2!!.tripKey)
    }

    @Test
    fun `parse Flash trip type`() {
        val texts = listOf(
            "Flash",
            "$ 900",
            "A 1 min (0.5 km)",
            "Calle Corta",
            "Viaje: 3 min (1.0 km)",
            "Cerca",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("Flash", trip!!.type)
    }

    @Test
    fun `parse Articulo trip type`() {
        val texts = listOf(
            "Artículo",
            "$ 1.100",
            "A 4 min (1.8 km)",
            "Calle Envío",
            "Viaje: 7 min (3.5 km)",
            "Depósito",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("Artículo", trip!!.type)
    }

    @Test
    fun `parse trip with subtype`() {
        val texts = listOf(
            "Moto",
            "Flash",
            "$ 800",
            "A 1 min (0.3 km)",
            "Esquina",
            "Viaje: 2 min (0.8 km)",
            "Cerca",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertTrue(trip!!.type == "Moto" || trip.type == "Flash")
        assertNotNull(trip.subtype)
    }

    @Test
    fun `pickup address extracted from next line`() {
        val texts = listOf(
            "Auto",
            "$ 2.000",
            "A 5 min (2.5 km)",
            "Av. Santa Fe 1234",
            "Viaje: 10 min (5.0 km)",
            "Recoleta",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("Av. Santa Fe 1234", trip!!.pickupAddress)
    }

    @Test
    fun `pickup address extracted from dash separator`() {
        val texts = listOf(
            "Auto",
            "$ 2.000",
            "A 5 min (2.5 km) - Av. Santa Fe 1234",
            "Viaje: 10 min (5.0 km)",
            "Recoleta",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("Av. Santa Fe 1234", trip!!.pickupAddress)
    }

    @Test
    fun `multiple prices picks closest to Aceptar`() {
        val texts = listOf(
            "$ 500",
            "Moto",
            "$ 1.800",
            "A 3 min (1.0 km)",
            "Calle Test",
            "Viaje: 8 min (4.0 km)",
            "Destino",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(1800.0, trip!!.price, 0.01)
    }

    @Test
    fun `only pickup no trip info still parses`() {
        val texts = listOf(
            "Moto",
            "$ 1.000",
            "A 2 min (1.0 km)",
            "Calle Test",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(2, trip!!.pickupMinutes)
        assertEquals(0, trip.tripMinutes)
    }

    @Test
    fun `only trip no pickup info still parses`() {
        val texts = listOf(
            "Auto",
            "$ 3.000",
            "Viaje: 15 min (7.0 km)",
            "Villa Crespo",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(0, trip!!.pickupMinutes)
        assertEquals(15, trip.tripMinutes)
    }

    // === ARS format tests ===

    @Test
    fun `parse UberX trip with ARS price`() {
        val texts = listOf(
            "UberX",
            "ARS1,476",
            "DNI verificado",
            "A 7 min (2.1 km)",
            "Unnamed Road, Resistencia",
            "Viaje: 4 min (1.0 km)",
            "Destino",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("UberX", trip!!.type)
        assertEquals(1476.0, trip.price, 0.01)
        assertEquals(7, trip.pickupMinutes)
        assertEquals(2.1, trip.pickupKm, 0.01)
        assertEquals(4, trip.tripMinutes)
        assertEquals(1.0, trip.tripKm, 0.01)
    }

    @Test
    fun `parse UberX Exclusivo with ARS price`() {
        val texts = listOf(
            "2 UberX Exclusivo",
            "ARS2,063",
            "DNI verificado * 4.95 (395)",
            "A 7 min (2.5 km)",
            "Avenida Las Heras, Resistencia",
            "Viaje: 7 min (3.1 km)",
            "Lisandro de la Torre 3080,",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("UberX", trip!!.type)
        assertEquals("Exclusivo", trip.subtype)
        assertEquals(2063.0, trip.price, 0.01)
    }

    @Test
    fun `parse ARS price with larger amount`() {
        val texts = listOf(
            "2 UberX",
            "ARS3,609",
            "Identidad digital verificada",
            "A 5 min (1.5 km)",
            "C. Lapacho, Barranqueras",
            "Viaje: 19 min (6.9 km)",
            "Destino",
            "Viaje disponible"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(3609.0, trip!!.price, 0.01)
        assertEquals(5, trip.pickupMinutes)
        assertEquals(19, trip.tripMinutes)
    }

    @Test
    fun `parse Encargo type`() {
        val texts = listOf(
            "Encargo",
            "Exclusivo",
            "$ 3.149",
            "Identidad digital verificada",
            "A 6 min (2.4 km)",
            "Avenida Warnes, CABA - Paternal",
            "Viaje: 13 min (4.0 km)",
            "Boyacá 731, CABA - Flores",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("Encargo", trip!!.type)
        assertEquals("Exclusivo", trip.subtype)
        assertEquals(3149.0, trip.price, 0.01)
    }

    // === OCR normalization tests ===

    @Test
    fun `normalize fixes l to 1 in pickup`() {
        assertEquals("A 1 min (0.2 km)", TripParser.normalizeOcrText("A l min (0.2 km)"))
        assertEquals("A 11 min (4.5 km)", TripParser.normalizeOcrText("A ll min (4.5 km)"))
        assertEquals("A 4 min (1.4 km)", TripParser.normalizeOcrText("A4 min (1.4 km)"))
    }

    @Test
    fun `normalize fixes l to 1 in km values`() {
        assertEquals("(1.1 km)", TripParser.normalizeOcrText("(1.l km)"))
        assertEquals("(3.1 km)", TripParser.normalizeOcrText("(3.l km)"))
    }

    @Test
    fun `normalize fixes missing decimal in 2-digit km`() {
        assertEquals("(1.3 km)", TripParser.normalizeOcrText("(13 km)"))
        assertEquals("(4.5 km)", TripParser.normalizeOcrText("(45 km)"))
    }

    @Test
    fun `normalize fixes dollar sign without space`() {
        assertEquals("$ 1.920", TripParser.normalizeOcrText("\$1.920"))
        assertEquals("$ 4.351", TripParser.normalizeOcrText("\$4.351"))
    }

    @Test
    fun `normalize fixes missing space after A`() {
        assertEquals("A 4 min (1.1 km)", TripParser.normalizeOcrText("A4 min (1.l km)"))
        assertEquals("A 7 min (2.0 km)", TripParser.normalizeOcrText("A7 min (2.0 km)"))
    }

    @Test
    fun `parse trip with OCR l-to-1 confusion in pickup`() {
        val texts = listOf(
            "Moto",
            "$ 2.523",
            "A l min (0.2 km)",
            "Avenida Federico Lacroze, CABA - Chacarita",
            "Viaje: 11 min (4.3 km)",
            "Armenia 2387, CABA - Palermo",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(1, trip!!.pickupMinutes)
        assertEquals(0.2, trip.pickupKm, 0.01)
    }

    @Test
    fun `parse trip with OCR ll-to-11 confusion`() {
        val texts = listOf(
            "Moto",
            "$ 2.756",
            "+\$ 199,00 incluido",
            "A ll min (4.5 km)",
            "Paz Soldán, CABA - Paternal",
            "Viaje: 11 min (4.0 km)",
            "Marcos Sastre 3581, CABA - Villa Del Parque",
            "Viaje disponible"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(11, trip!!.pickupMinutes)
        assertEquals(4.5, trip.pickupKm, 0.01)
        assertEquals(199.0, trip.bonus!!, 0.01)
    }

    @Test
    fun `parse trip with OCR missing decimal in km`() {
        val texts = listOf(
            "Moto",
            "$ 1.620",
            "DNI verificado",
            "A5 min (13 km)",
            "Céspedes, CABA - Colegiales",
            "Viaje: 8 min (2.3 km)",
            "Arribeños 2516, CABA - Belgrano",
            "Viaje disponible"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(5, trip!!.pickupMinutes)
        assertEquals(1.3, trip.pickupKm, 0.01)
    }

    @Test
    fun `parse trip with OCR l in km decimal`() {
        val texts = listOf(
            "Artículo Exclusivo",
            "$ 2.886",
            "Pasaporte verificado",
            "A4 min (1.l km)",
            "Avenida Jorge Newbery, CABA - Chacarita",
            "Viaje: 15 min (4.8 km)",
            "Avenida Monroe 4562, CABA - Villa Urquiza",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(4, trip!!.pickupMinutes)
        assertEquals(1.1, trip.pickupKm, 0.01)
    }

    @Test
    fun `rating with dot format also matches`() {
        val texts = listOf(
            "UberX",
            "ARS2,514",
            "DNI verificado * 4.94 (313)",
            "A 3 min (1.3 km)",
            "Nicolás Acosta",
            "Viaje: 10 min (5.0 km)",
            "Avenida España, Barranqueras",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertNotNull(trip!!.passengerRating)
    }

    @Test
    fun `Viaje disponible works as anchor like Aceptar`() {
        val texts = listOf(
            "Moto",
            "$ 1.701",
            "A 4 min (1.4 km)",
            "Avenida Dorrego, CABA - Chacarita",
            "Viaje: 7 min (2.4 km)",
            "Avenida Corrientes 4464, CABA - Almagro",
            "Viaje disponible"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(1701.0, trip!!.price, 0.01)
    }

    // === Speed heuristic tests ===

    @Test
    fun `speed heuristic corrects l=1 to 11 when km is high`() {
        // OCR reads "A l min (4.6 km)" → normalizes to "A 1 min (4.6 km)"
        // 4.6 km in 1 min = 276 km/h → absurd → must be 11 min
        val texts = listOf(
            "Moto",
            "$ 2.341",
            "A l min (4.6 km)",
            "Holmberg, CABA - Villa Urquiza",
            "Viaje: 9 min (4.0 km)",
            "Palermo",
            "Viaje disponible"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(11, trip!!.pickupMinutes)
        assertEquals(4.6, trip.pickupKm, 0.01)
    }

    @Test
    fun `speed heuristic keeps 1 min when km is low`() {
        // 0.2 km in 1 min = 12 km/h → reasonable → keep 1
        val texts = listOf(
            "Moto",
            "$ 2.523",
            "A l min (0.2 km)",
            "Avenida Federico Lacroze",
            "Viaje: 11 min (4.3 km)",
            "Armenia 2387",
            "Aceptar"
        )
        val trip = TripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(1, trip!!.pickupMinutes)
        assertEquals(0.2, trip.pickupKm, 0.01)
    }
}
