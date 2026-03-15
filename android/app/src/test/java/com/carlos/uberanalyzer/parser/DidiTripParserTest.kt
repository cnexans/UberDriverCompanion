package com.carlos.uberanalyzer.parser

import org.junit.Assert.*
import org.junit.Test

class DidiTripParserTest {

    // === Basic Didi trip parsing ===

    @Test
    fun `parse basic Didi trip with full info`() {
        // Based on screenshot: $12.774, pickup 5min 1,2km, trip 20min 10,1km
        val texts = listOf(
            "Efectivo",
            "$ 12.774",
            "$ 1.263 incluidos",
            "3",
            "4,96 · 296 arrendamientos",
            "Tarjeta bancaria verificada",
            "5min (1,2km)",
            "Conjunto Residencial Villa Allegra, Comuna 17",
            "20min (10,1km)",
            "Calle 54 # 26H-19, Comuna 12"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("Didi", trip!!.type)
        assertEquals(12774.0, trip.price, 0.01)
        assertEquals(1263.0, trip.bonus!!, 0.01)
        assertEquals(5, trip.pickupMinutes)
        assertEquals(1.2, trip.pickupKm, 0.01)
        assertEquals("Conjunto Residencial Villa Allegra, Comuna 17", trip.pickupAddress)
        assertEquals(20, trip.tripMinutes)
        assertEquals(10.1, trip.tripKm, 0.01)
        assertEquals("Calle 54 # 26H-19, Comuna 12", trip.destination)
    }

    @Test
    fun `parse Didi trip 11607`() {
        // Based on screenshot: $11.607, 3min 760m->0.76km, 23min 11.2km
        val texts = listOf(
            "Efectivo",
            "$ 11.607",
            "$ 1.400 incluidos",
            "3",
            "5,00 · 11 arrendamientos",
            "Tarjeta bancaria verificada",
            "3min (0,8km)",
            "Aquarela Centro Comercial, Comuna 17",
            "23min (11,2km)",
            "La Estacion Centro Comercial, Comuna 4"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(11607.0, trip!!.price, 0.01)
        assertEquals(1400.0, trip.bonus!!, 0.01)
        assertEquals(3, trip.pickupMinutes)
        assertEquals(0.8, trip.pickupKm, 0.01)
        assertEquals(23, trip.tripMinutes)
        assertEquals(11.2, trip.tripKm, 0.01)
    }

    @Test
    fun `parse Didi trip 9812`() {
        val texts = listOf(
            "Efectivo",
            "$ 9.812",
            "$ 700 incluidos",
            "3",
            "4,97 · 683 arrendamientos",
            "Tarjeta bancaria verificada",
            "8min (2,7km)",
            "Calle 33a # 66-9, Comuna 16",
            "19min (5,6km)",
            "Clinica Rey David, Comuna 19"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(9812.0, trip!!.price, 0.01)
        assertEquals(700.0, trip.bonus!!, 0.01)
        assertEquals(8, trip.pickupMinutes)
        assertEquals(2.7, trip.pickupKm, 0.01)
        assertEquals(19, trip.tripMinutes)
        assertEquals(5.6, trip.tripKm, 0.01)
    }

    @Test
    fun `parse Didi trip 8085`() {
        val texts = listOf(
            "Efectivo",
            "$ 8.085",
            "$ 675 incluidos",
            "3",
            "4,98 · 44 arrendamientos",
            "4min (0,7km)",
            "Unidad Residencial Parque Krabi, Comuna 17",
            "18min (5,4km)",
            "Conjunto Residencial Banyo, Comuna 17"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(8085.0, trip!!.price, 0.01)
        assertEquals(675.0, trip.bonus!!, 0.01)
        assertEquals(4, trip.pickupMinutes)
        assertEquals(0.7, trip.pickupKm, 0.01)
        assertEquals(18, trip.tripMinutes)
        assertEquals(5.4, trip.tripKm, 0.01)
    }

    @Test
    fun `parse Didi trip 10472`() {
        val texts = listOf(
            "Efectivo",
            "$ 10.472",
            "$ 725 incluidos",
            "3",
            "4,90 · 127 arrendamientos",
            "2min (0,2km)",
            "condominio villa campestre del rio 2, Comuna 17",
            "20min (5,8km)",
            "Conjunto Residencial Cian, El Hormiguero"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(10472.0, trip!!.price, 0.01)
        assertEquals(2, trip.pickupMinutes)
        assertEquals(0.2, trip.pickupKm, 0.01)
        assertEquals(20, trip.tripMinutes)
        assertEquals(5.8, trip.tripKm, 0.01)
    }

    @Test
    fun `parse Didi trip with stops`() {
        // Based on screenshot with "1 parada(s)"
        val texts = listOf(
            "Efectivo",
            "$ 13.090",
            "$ 1.175 incluidos",
            "3",
            "4,95 · 540 arrendamientos",
            "4min (0,8km)",
            "Paseo de las Casas 1, Comuna 17",
            "1 parada(s)",
            "32min (9,4km)",
            "Paseo de las Casas 1, Comuna 17"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(13090.0, trip!!.price, 0.01)
        assertEquals(4, trip.pickupMinutes)
        assertEquals(0.8, trip.pickupKm, 0.01)
        assertEquals(32, trip.tripMinutes)
        assertEquals(9.4, trip.tripKm, 0.01)
        assertEquals("1 parada(s)", trip.subtype)
    }

    @Test
    fun `parse Didi trip 10857`() {
        val texts = listOf(
            "Efectivo",
            "$ 10.857",
            "$ 913 incluidos",
            "3",
            "4,97 · 60 arrendamientos",
            "8min (2,6km)",
            "Tienda Super R, Comuna 17",
            "24min (7,3km)",
            "Carrera 46 #4a-41, Comuna 19"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(10857.0, trip!!.price, 0.01)
        assertEquals(913.0, trip.bonus!!, 0.01)
        assertEquals(8, trip.pickupMinutes)
        assertEquals(2.6, trip.pickupKm, 0.01)
        assertEquals(24, trip.tripMinutes)
        assertEquals(7.3, trip.tripKm, 0.01)
    }

    @Test
    fun `parse Didi trip 10315`() {
        val texts = listOf(
            "Efectivo",
            "$ 10.315",
            "$ 1.200 incluidos",
            "3",
            "4,92 · 130 arrendamientos",
            "5min (1,3km)",
            "Atencion Al Cliente Emcali, Comuna 17",
            "17min (9,6km)",
            "Autoservicio Barberena, Comuna 12"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(10315.0, trip!!.price, 0.01)
        assertEquals(1200.0, trip.bonus!!, 0.01)
        assertEquals(5, trip.pickupMinutes)
        assertEquals(1.3, trip.pickupKm, 0.01)
        assertEquals(17, trip.tripMinutes)
        assertEquals(9.6, trip.tripKm, 0.01)
    }

    @Test
    fun `parse Didi trip 5698`() {
        val texts = listOf(
            "Efectivo",
            "$ 5.698",
            "$ 275 incluidos",
            "3",
            "4,96 · 265 arrendamientos",
            "3min (0,2km)",
            "Carrera 76 #14c-53, Comuna 17",
            "9min (2,2km)",
            "Manzanares del Lili, Comuna 17"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(5698.0, trip!!.price, 0.01)
        assertEquals(275.0, trip.bonus!!, 0.01)
        assertEquals(3, trip.pickupMinutes)
        assertEquals(0.2, trip.pickupKm, 0.01)
        assertEquals(9, trip.tripMinutes)
        assertEquals(2.2, trip.tripKm, 0.01)
    }

    @Test
    fun `parse Didi trip 8714`() {
        val texts = listOf(
            "Efectivo",
            "$ 8.714",
            "$ 625 incluidos",
            "3",
            "4,96 · 567 arrendamientos",
            "8min (2,7km)",
            "Calle 33a #67a-32, Comuna 16",
            "17min (5km)",
            "\"La Cascada\", Comuna 19"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(8714.0, trip!!.price, 0.01)
        assertEquals(625.0, trip.bonus!!, 0.01)
        assertEquals(8, trip.pickupMinutes)
        assertEquals(2.7, trip.pickupKm, 0.01)
        assertEquals(17, trip.tripMinutes)
        assertEquals(5.0, trip.tripKm, 0.01)
    }

    // === Detection tests ===

    @Test
    fun `isDidi detects Didi texts`() {
        val didiTexts = listOf(
            "Efectivo",
            "$ 12.774",
            "$ 1.263 incluidos",
            "4,96 · 296 arrendamientos"
        )
        assertTrue(DidiTripParser.isDidi(didiTexts))
    }

    @Test
    fun `isDidi rejects Uber texts`() {
        val uberTexts = listOf(
            "Moto",
            "$ 1.500",
            "A 3 min (1.2 km)",
            "Viaje: 10 min (5.0 km)",
            "Aceptar"
        )
        assertFalse(DidiTripParser.isDidi(uberTexts))
    }

    // === Rating parsing ===

    @Test
    fun `rating extracted correctly`() {
        val texts = listOf(
            "Efectivo",
            "$ 8.000",
            "4,96 · 296 arrendamientos",
            "5min (1,0km)",
            "Lugar A",
            "10min (5,0km)",
            "Lugar B"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertNotNull(trip!!.passengerRating)
        assertTrue(trip.passengerRating!!.contains("4,96"))
        assertTrue(trip.passengerRating!!.contains("296"))
    }

    // === Identity tests ===

    @Test
    fun `identity tarjeta bancaria detected`() {
        val texts = listOf(
            "Efectivo",
            "$ 8.000",
            "Tarjeta bancaria verificada",
            "5min (1,0km)",
            "Lugar A",
            "10min (5,0km)",
            "Lugar B"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals("Tarjeta bancaria verificada", trip!!.identity)
    }

    // === Metric calculations ===

    @Test
    fun `pesosPorKm calculated correctly for Didi`() {
        val texts = listOf(
            "Efectivo",
            "$ 10.000",
            "5min (2,0km)",
            "Lugar A",
            "15min (8,0km)",
            "Lugar B"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        // totalKm = 2.0 + 8.0 = 10.0, price = 10000, $/km = 1000
        assertEquals(1000.0, trip!!.pesosPorKm, 0.01)
    }

    @Test
    fun `pctDistancia calculated correctly for Didi`() {
        val texts = listOf(
            "Efectivo",
            "$ 10.000",
            "5min (2,0km)",
            "Lugar A",
            "15min (8,0km)",
            "Lugar B"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        // tripKm = 8.0, totalKm = 10.0, pct = 80%
        assertEquals(80.0, trip!!.pctDistancia, 0.01)
    }

    // === Edge cases ===

    @Test
    fun `returns null when no time-distance info`() {
        val texts = listOf(
            "Efectivo",
            "$ 12.774",
            "4,96 · 296 arrendamientos"
        )
        val trip = DidiTripParser.parse(texts)
        assertNull(trip)
    }

    @Test
    fun `returns null when no price`() {
        val texts = listOf(
            "Efectivo",
            "5min (1,2km)",
            "Lugar A",
            "20min (10,1km)",
            "Lugar B"
        )
        val trip = DidiTripParser.parse(texts)
        assertNull(trip)
    }

    @Test
    fun `only pickup no trip still parses`() {
        val texts = listOf(
            "Efectivo",
            "$ 5.000",
            "5min (1,2km)",
            "Lugar A"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(5, trip!!.pickupMinutes)
        assertEquals(1.2, trip.pickupKm, 0.01)
        assertEquals(0, trip.tripMinutes)
    }

    // === Meters format ===

    @Test
    fun `parse trip with pickup in meters`() {
        val texts = listOf(
            "Efectivo",
            "$ 11.607",
            "$ 1.400 incluidos",
            "3min (760m)",
            "Aquarela Centro Comercial, Comuna 17",
            "23min (11,2km)",
            "La Estacion Centro Comercial, Comuna 4"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(3, trip!!.pickupMinutes)
        assertEquals(0.76, trip.pickupKm, 0.01)
        assertEquals(23, trip.tripMinutes)
        assertEquals(11.2, trip.tripKm, 0.01)
    }

    @Test
    fun `parse trip with pickup 199m`() {
        val texts = listOf(
            "$ 10.472",
            "2min (199m)",
            "condominio villa campestre del rio 2, Comuna 17",
            "20min (5,8km)",
            "Conjunto Residencial Cian, El Hormiguero"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(2, trip!!.pickupMinutes)
        assertEquals(0.199, trip.pickupKm, 0.01)
        assertEquals(20, trip.tripMinutes)
    }

    @Test
    fun `parse trip with pickup 689m`() {
        val texts = listOf(
            "$ 8.085",
            "4min (689m)",
            "Unidad Residencial Parque Krabi, Comuna 17",
            "18min (5,4km)",
            "Conjunto Residencial Banyo, Comuna 17"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(4, trip!!.pickupMinutes)
        assertEquals(0.689, trip.pickupKm, 0.01)
        assertEquals(18, trip.tripMinutes)
        assertEquals(5.4, trip.tripKm, 0.01)
    }

    // === OCR normalization ===

    @Test
    fun `normalize fixes S to 5 in min context`() {
        assertEquals("5min (1,2km)", DidiTripParser.normalizeOcrText("Smin (1,2km)"))
    }

    @Test
    fun `normalize fixes dollar sign without space`() {
        assertEquals("$ 12.774", DidiTripParser.normalizeOcrText("\$12.774"))
    }

    @Test
    fun `normalize fixes l to 1 in min context`() {
        assertEquals("5min (1,2km)", DidiTripParser.normalizeOcrText("5min (1,2km)"))
    }

    @Test
    fun `normalize removes O prefix artifact`() {
        assertEquals("5min (1,2km)", DidiTripParser.normalizeOcrText("O 5min (1,2km)"))
        assertEquals("Tarjeta bancaria verificada", DidiTripParser.normalizeOcrText("O Tarjeta bancaria verificada"))
    }

    // === Colombian number format ===

    @Test
    fun `colombian price with dots parsed correctly`() {
        val texts = listOf(
            "$ 12.774",
            "5min (1,0km)",
            "Lugar A",
            "10min (5,0km)",
            "Lugar B"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(12774.0, trip!!.price, 0.01)
    }

    @Test
    fun `colombian km with comma parsed correctly`() {
        val texts = listOf(
            "$ 5.000",
            "5min (1,2km)",
            "Lugar A",
            "10min (10,1km)",
            "Lugar B"
        )
        val trip = DidiTripParser.parse(texts)
        assertNotNull(trip)
        assertEquals(1.2, trip!!.pickupKm, 0.01)
        assertEquals(10.1, trip.tripKm, 0.01)
    }
}
