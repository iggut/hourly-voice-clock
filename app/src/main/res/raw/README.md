# Chime Sound Resources

This directory contains audio files for the chime sounds available in the Hourly Voice Clock app.

## Available Sound Files

- `bell.ogg` - Bell chime
- `bird_chirp.ogg` - Bird chirping sound
- `classic_chime.ogg` - Classic clock chime
- `cymbals.ogg` - Cymbals clash
- `digital_beep.ogg` - Digital beep tone
- `gong.ogg` - Gong strike
- `honk.ogg` - Honk sound

## Resource ID Mapping

The `TimeAnnouncer.getChimeResourceId()` method maps `ChimeSound` enum values to these raw resources:

| ChimeSound        | Resource ID          |
|-------------------|----------------------|
| NONE              | (returns early)      |
| CLASSIC_CHIME     | R.raw.classic_chime  |
| BELL              | R.raw.bell           |
| GONG              | R.raw.gong           |
| CYMBALS           | R.raw.cymbals        |
| DIGITAL_BEEP      | R.raw.digital_beep   |
| BIRD_CHIRP        | R.raw.bird_chirp     |
| HONK              | R.raw.honk           |
