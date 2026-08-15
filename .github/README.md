
# Light Transit

A transit app for the Light Phone III.

<img width="2572" height="925" alt="transit" src="https://github.com/user-attachments/assets/98f07502-f93c-449b-9b96-0bd80cb0c983" />

## Features

<img height="508" alt="image" src="./assets/worldwide-gtfs-support.png" />

<img height="508" alt="image" src="./assets/nearby-stops.png" />

<img height="508" alt="image" src="./assets/nearby-routes.png" />

<img height="508" alt="image" src="./assets/multimodal-routing.gif" />

## Usage and installation

Currently, the APK must be compiled.

Obtain a TransitAPI key, and in `local.properties`:

```sh
TRANSIT_API_KEY=YourKeyHere
```

## Roadmap

- Implemented
  - Global GTFS support
  - View stops, routes, and scheduled departures from locally-downloaded GTFS feeds
  - Multimodal routing with user-provided TransitAPI key
- Planned
  - Realtime vehicle and schedule updates
  - Multimodal routing *without* requiring an API key
  - Shared micromobility support (e.g. bikeshare)
  - Vector map tile support
  - Live position and heading indicators
