
# Light Transit

A transit app for the Light Phone III.

<img width="2572" height="925" alt="transit" src="https://github.com/user-attachments/assets/98f07502-f93c-449b-9b96-0bd80cb0c983" />

## What's the difference between this and PicoTransit?

tl;dr you probably want [PicoTransit](https://github.com/CJFData/light-transit/tree/pico-transit). (It's great!)

This uses the [Transit API](https://transitapp.com/partners/apis), which offers neat features like advanced multimodal routing, bikeshare availability, live updates, and support for 1200+ cities out of the box. *However,* the API is not free for production use. A developer API key is free but only enough for one user, and it must be formally requested from Transit (see link above).

PicoTransit's focus is less on routing (at the time of writing), and it does not use an API; instead it implements support for transit agencies individually. As such it is significantly more accessible.

## Prerequisites

Currently, the APK must be compiled.

Obtain a TransitAPI key, and in `local.properties`:

```sh
TRANSIT_API_KEY=YourKeyHere
```

## Features

<img height="508" alt="image" src="./assets/worldwide-gtfs-support.png" />

<img height="508" alt="image" src="./assets/nearby-stops.png" />

<img height="508" alt="image" src="./assets/nearby-routes.png" />

<img height="508" alt="image" src="./assets/multimodal-routing.gif" />
