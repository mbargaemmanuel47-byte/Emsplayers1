package com.example.data.local

import com.example.model.AiMoodPreset
import com.example.model.LyricLine
import com.example.model.SynthProfile
import com.example.model.Track

object SeedCatalog {

    val moodPresets: List<AiMoodPreset> = listOf(
        AiMoodPreset(
            id = "focus",
            title = "Mix Focus & Flow",
            subtitle = "Concentration profonde & ondes alpha",
            bpmRange = "90 - 98 BPM",
            badge = "SYNTHÈSE FOCUS",
            moodFilter = "focus",
            trackId = 2L
        ),
        AiMoodPreset(
            id = "zouk",
            title = "Zouk & R&B Nocturne",
            subtitle = "Groove velouté & basses chaleureuses",
            bpmRange = "98 - 106 BPM",
            badge = "TENDANCE SOIRÉE",
            moodFilter = "zouk",
            trackId = 1L
        ),
        AiMoodPreset(
            id = "studio",
            title = "Énergie Studio",
            subtitle = "Percussions 808 & dynamique haute",
            bpmRange = "120 - 128 BPM",
            badge = "HIGH ENERGY",
            moodFilter = "studio",
            trackId = 4L
        ),
        AiMoodPreset(
            id = "chill",
            title = "Chill & Synthwave",
            subtitle = "Nappes analogiques & évasion nocturne",
            bpmRange = "105 - 114 BPM",
            badge = "SPATIAL 3D",
            moodFilter = "chill",
            trackId = 3L
        )
    )

    val initialTracks: List<Track> = listOf(
        Track(
            id = 1L,
            title = "Velours d'Abidjan",
            artist = "Ems SoundLab & Kévin K.",
            album = "Nocturne Tropicale",
            genre = "Zouk & R&B",
            mood = "zouk",
            bpm = 104,
            musicalKey = "Fm",
            energy = 78,
            durationSec = 204,
            isLiked = true,
            primaryColorHex = 0xFFD4AF37,
            secondaryColorHex = 0xFF1E3A8A,
            tertiaryColorHex = 0xFF090D16,
            synthProfile = SynthProfile(
                scaleHz = listOf(174.61, 207.65, 261.63, 311.13, 349.23, 415.30),
                bassHz = listOf(87.31, 65.41, 77.78, 69.30),
                waveType = "triangle",
                rhythmPattern = listOf(1f, 0f, 0.5f, 1f, 0f, 0.7f, 1f, 0.4f),
                groove = 86,
                warmth = 92,
                clarity = 88
            ),
            aiInsight = "Rythmique zouk syncopée à 104 BPM soutenue par une basse sub-harmonique en Fa mineur et des nappes Rhodes veloutées.",
            lyrics = listOf(
                LyricLine(0, "Sous les lumières dorées de la lagune ce soir..."),
                LyricLine(18, "Le rythme ralentit, nos pensées s'envolent dans le noir"),
                LyricLine(38, "Chaque pulsation résonne comme un écho d'or et de bleu"),
                LyricLine(60, "Laisse la fréquence guider nos pas, loin du bruit silencieux"),
                LyricLine(85, "Dans l'aura de la nuit, tout devient fluide et précieux"),
                LyricLine(115, "Harmonie suspendue entre deux battements de cœur"),
                LyricLine(150, "Encore une danse sous les étoiles de minuit...")
            )
        ),
        Track(
            id = 2L,
            title = "Horizons Dorés (Neural Flow)",
            artist = "Ems SoundLab",
            album = "Deep Focus Architecture",
            genre = "Focus & Ambient",
            mood = "focus",
            bpm = 92,
            musicalKey = "Cm",
            energy = 54,
            durationSec = 228,
            isLiked = true,
            primaryColorHex = 0xFFFACC15,
            secondaryColorHex = 0xFF0F172A,
            tertiaryColorHex = 0xFF1D4ED8,
            synthProfile = SynthProfile(
                scaleHz = listOf(261.63, 311.13, 392.00, 466.16, 523.25),
                bassHz = listOf(65.41, 77.78, 58.27, 65.41),
                waveType = "sine",
                rhythmPattern = listOf(1f, 0f, 0f, 0.4f, 0.8f, 0f, 0.3f, 0f),
                groove = 68,
                warmth = 95,
                clarity = 94
            ),
            aiInsight = "Ondes alpha calibrées à 92 BPM avec accords de neuvième mineure favorisant la concentration profonde sans fatigue auditive.",
            lyrics = listOf(
                LyricLine(0, "[Séquence harmonique synthétisée — Calibration Focus]"),
                LyricLine(25, "Respire au rythme des nappes cristallines en Do mineur"),
                LyricLine(55, "L'esprit s'éclaircit, chaque idée trouve sa trajectoire"),
                LyricLine(95, "Stabilité acoustique • Flux continu 48kHz"),
                LyricLine(140, "Élévation douce vers un état de créativité pure")
            )
        ),
        Track(
            id = 3L,
            title = "Nébuleuse Synthétique",
            artist = "Léna V.",
            album = "Midnight Drive 2084",
            genre = "Chill & Synthwave",
            mood = "chill",
            bpm = 110,
            musicalKey = "Am",
            energy = 72,
            durationSec = 196,
            isLiked = false,
            primaryColorHex = 0xFFF59E0B,
            secondaryColorHex = 0xFF312E81,
            tertiaryColorHex = 0xFF06070B,
            synthProfile = SynthProfile(
                scaleHz = listOf(220.00, 261.63, 329.63, 392.00, 493.88),
                bassHz = listOf(110.00, 87.31, 98.00, 82.41),
                waveType = "sawtooth",
                rhythmPattern = listOf(1f, 0.4f, 0.8f, 0.4f, 1f, 0.4f, 0.8f, 0.6f),
                groove = 82,
                warmth = 85,
                clarity = 90
            ),
            aiInsight = "Arpèges analogiques rétro-futuristes en La mineur combinés à une réverbération spatiale large.",
            lyrics = listOf(
                LyricLine(0, "Horizon électrique sur l'autoroute de minuit"),
                LyricLine(22, "Les reflets d'or dansent sur le capot mouillé"),
                LyricLine(48, "On traverse le temps porté par le synthétiseur"),
                LyricLine(80, "Aucune destination, juste l'instant présent"),
                LyricLine(120, "Échos analogiques jusqu'aux premières lueurs de l'aube")
            )
        ),
        Track(
            id = 4L,
            title = "Impulsion Studio 808",
            artist = "Mael Beats",
            album = "Gold Standard Sessions",
            genre = "Énergie Studio",
            mood = "studio",
            bpm = 124,
            musicalKey = "Gm",
            energy = 91,
            durationSec = 185,
            isLiked = true,
            primaryColorHex = 0xFFEAB308,
            secondaryColorHex = 0xFF1E1B4B,
            tertiaryColorHex = 0xFF090D16,
            synthProfile = SynthProfile(
                scaleHz = listOf(196.00, 233.08, 293.66, 349.23, 440.00),
                bassHz = listOf(98.00, 73.42, 87.31, 65.41),
                waveType = "square",
                rhythmPattern = listOf(1f, 0.6f, 1f, 0.8f, 1f, 0.5f, 1f, 0.9f),
                groove = 96,
                warmth = 80,
                clarity = 87
            ),
            aiInsight = "Signature rythmique percussive haute énergie à 124 BPM conçue pour stimuler la motivation et le mouvement.",
            lyrics = listOf(
                LyricLine(0, "Montée en pression dans le studio, le signal est au vert"),
                LyricLine(20, "Basse 808 calibrée, précision chirurgicale"),
                LyricLine(45, "On repousse les limites du son, fréquence après fréquence"),
                LyricLine(75, "Énergie maximale • Dynamique dorée"),
                LyricLine(115, "Le final éclate dans une cascade harmonique")
            )
        ),
        Track(
            id = 5L,
            title = "Étoile de Minuit (Zouk Love)",
            artist = "Sora",
            album = "Nuits d'Ébène",
            genre = "Zouk & R&B",
            mood = "zouk",
            bpm = 98,
            musicalKey = "Dm",
            energy = 69,
            durationSec = 215,
            isLiked = false,
            primaryColorHex = 0xFFFDE68A,
            secondaryColorHex = 0xFF1E3A8A,
            tertiaryColorHex = 0xFF06070B,
            synthProfile = SynthProfile(
                scaleHz = listOf(293.66, 349.23, 440.00, 523.25, 659.25),
                bassHz = listOf(73.42, 87.31, 65.41, 55.00),
                waveType = "triangle",
                rhythmPattern = listOf(1f, 0f, 0.6f, 1f, 0f, 0.6f, 1f, 0.3f),
                groove = 88,
                warmth = 96,
                clarity = 91
            ),
            aiInsight = "Ballade Zouk-R&B intime à 98 BPM mettant en valeur des harmonies vocales chaudes et un groove chaloupé.",
            lyrics = listOf(
                LyricLine(0, "Quand la ville s'endort, ta voix reste en lumière"),
                LyricLine(24, "Un pas à deux sur la cadence douce de l'océan"),
                LyricLine(52, "Nos silences parlent plus fort que tous les discours"),
                LyricLine(88, "Sous cette étoile d'or, le temps s'arrête pour nous"),
                LyricLine(130, "Douceur infinie jusqu'au lever du jour...")
            )
        ),
        Track(
            id = 6L,
            title = "Onde Solaire & Café",
            artist = "Ems Morning",
            album = "Clarté Matinale",
            genre = "Focus & Ambient",
            mood = "focus",
            bpm = 96,
            musicalKey = "Fmaj7",
            energy = 62,
            durationSec = 192,
            isLiked = false,
            primaryColorHex = 0xFFFACC15,
            secondaryColorHex = 0xFF0C4A6E,
            tertiaryColorHex = 0xFF06070B,
            synthProfile = SynthProfile(
                scaleHz = listOf(349.23, 440.00, 523.25, 659.25, 783.99),
                bassHz = listOf(87.31, 110.00, 65.41, 98.00),
                waveType = "sine",
                rhythmPattern = listOf(1f, 0.2f, 0.5f, 0.2f, 0.9f, 0.2f, 0.6f, 0.3f),
                groove = 74,
                warmth = 93,
                clarity = 95
            ),
            aiInsight = "Accords majeurs lumineux (Fmaj9) favorisant une humeur positive et une clarté mentale immédiate.",
            lyrics = listOf(
                LyricLine(0, "Premiers rayons dorés à travers la fenêtre"),
                LyricLine(28, "Une mélodie légère accompagne le réveil des sens"),
                LyricLine(65, "Équilibre parfait entre calme et élan créatif"),
                LyricLine(110, "Chaque note éclaire la journée qui commence")
            )
        )
    )
}
