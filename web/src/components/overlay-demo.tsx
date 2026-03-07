"use client";

import { useEffect, useState } from "react";

type Level = "green" | "yellow" | "red";

const trips: {
  type: string;
  price: string;
  porKm: { value: string; good: boolean };
  porHora: { value: string; good: boolean };
  pctDist: { value: string; level: Level };
  pickup: string;
  trip: string;
}[] = [
  {
    type: "Moto",
    price: "$ 1.738",
    porKm: { value: "378", good: true },
    porHora: { value: "8.690", good: true },
    pctDist: { value: "41%", level: "yellow" },
    pickup: "7min (2.7km)",
    trip: "5min (1.9km)",
  },
  {
    type: "Auto Exclusivo",
    price: "$ 4.520",
    porKm: { value: "502", good: true },
    porHora: { value: "11.300", good: true },
    pctDist: { value: "67%", level: "green" },
    pickup: "5min (3.0km)",
    trip: "12min (6.0km)",
  },
  {
    type: "Moto",
    price: "$ 984",
    porKm: { value: "164", good: false },
    porHora: { value: "3.936", good: false },
    pctDist: { value: "33%", level: "red" },
    pickup: "8min (4.0km)",
    trip: "7min (2.0km)",
  },
];

export function OverlayDemo() {
  const [idx, setIdx] = useState(0);
  const [flash, setFlash] = useState(false);

  useEffect(() => {
    const interval = setInterval(() => {
      setFlash(true);
      setTimeout(() => {
        setIdx((i) => (i + 1) % trips.length);
        setFlash(false);
      }, 150);
    }, 4000);
    return () => clearInterval(interval);
  }, []);

  const trip = trips[idx];
  const pctColor =
    trip.pctDist.level === "green"
      ? "text-green-400"
      : trip.pctDist.level === "yellow"
        ? "text-yellow-400"
        : "text-red-400";

  return (
    <div className="relative">
      {/* Phone frame */}
      <div className="w-[280px] h-[560px] bg-neutral-900 rounded-[2.5rem] border-4 border-neutral-700 shadow-2xl shadow-black/50 overflow-hidden relative">
        {/* Status bar */}
        <div className="h-8 bg-black flex items-center justify-center">
          <div className="w-20 h-4 bg-neutral-800 rounded-full" />
        </div>

        {/* Fake map */}
        <div className="absolute inset-0 top-8 bg-gradient-to-b from-neutral-800 via-neutral-850 to-neutral-900">
          <div className="absolute inset-0 opacity-20">
            {[...Array(8)].map((_, i) => (
              <div
                key={`h-${i}`}
                className="absolute h-px bg-neutral-500"
                style={{
                  top: `${12 + i * 12}%`,
                  left: "5%",
                  right: "5%",
                  transform: `rotate(${-5 + i * 3}deg)`,
                }}
              />
            ))}
            {[...Array(6)].map((_, i) => (
              <div
                key={`v-${i}`}
                className="absolute w-px bg-neutral-500"
                style={{
                  left: `${15 + i * 14}%`,
                  top: "5%",
                  bottom: "30%",
                  transform: `rotate(${2 + i * 2}deg)`,
                }}
              />
            ))}
          </div>
          {/* Purple zone */}
          <div className="absolute bottom-[25%] left-[10%] right-[15%] top-[30%] bg-purple-500/10 rounded-lg" />
        </div>

        {/* Overlay widget */}
        <div
          className={`absolute top-12 left-3 right-3 transition-opacity duration-150 ${flash ? "opacity-0" : "opacity-100"}`}
        >
          <div className="bg-black/90 backdrop-blur-sm rounded-lg px-4 py-3 border border-neutral-700/50 shadow-lg">
            <div className="text-green-400 text-[10px] font-bold text-center tracking-widest mb-1">
              ▬ UBER ▬
            </div>
            <div className="text-neutral-500 text-[10px]">{trip.type}</div>
            <div className="text-white text-xl font-bold">{trip.price}</div>
            <div className="flex gap-4 mt-1">
              <span
                className={`text-xs font-bold ${trip.porKm.good ? "text-green-400" : "text-red-400"}`}
              >
                $/km: {trip.porKm.value}
              </span>
              <span
                className={`text-xs font-bold ${trip.porHora.good ? "text-green-400" : "text-red-400"}`}
              >
                $/h: {trip.porHora.value}
              </span>
            </div>
            <div className={`text-xs font-bold mt-0.5 ${pctColor}`}>
              Dist cobrada: {trip.pctDist.value}
            </div>
            <div className="text-neutral-500 text-[10px] mt-1">
              Recogida: {trip.pickup}
            </div>
            <div className="text-neutral-500 text-[10px]">
              Viaje: {trip.trip}
            </div>
          </div>
        </div>

        {/* Bottom bar */}
        <div className="absolute bottom-0 inset-x-0 h-16 bg-black/80 backdrop-blur flex items-center justify-center">
          <div className="w-32 h-1 bg-neutral-600 rounded-full" />
        </div>
      </div>

      {/* Cycling dots */}
      <div className="flex justify-center gap-2 mt-4">
        {trips.map((_, i) => (
          <div
            key={i}
            className={`w-2 h-2 rounded-full transition-colors ${i === idx ? "bg-green-400" : "bg-neutral-700"}`}
          />
        ))}
      </div>
    </div>
  );
}
