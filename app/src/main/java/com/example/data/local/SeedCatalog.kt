package com.example.data.local

import com.example.model.Track

/**
 * Em Player fonctionne exclusivement avec les vraies pistes audio du stockage de l'appareil.
 * Aucun morceau de démonstration n'est préchargé.
 */
object SeedCatalog {
    val initialTracks: List<Track> = emptyList()
}
