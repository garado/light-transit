
# Light Transit

A transit app for the Light Phone III.

## What's the difference between this and PicoTransit?

tl;dr you probably want [PicoTransit](https://github.com/CJFData/light-transit/tree/pico-transit). (It's great!)

This uses the [Transit API](https://transitapp.com/partners/apis), which offers neat features like advanced multimodal routing, bikeshare availability, live updates, and support for 1200+ cities out of the box. *However,* the API is not free for production use. A developer API key is free but only enough for one user, and it must be formally requested from Transit (see link above).

PicoTransit's focus is less on routing (at the time of writing), and it does not use an API; instead it implements support for transit agencies individually.

## Prerequisites

Currently, the APK must be compiled.

Obtain a TransitAPI key, and in `local.properties`:

```sh
TRANSIT_API_KEY=YourKeyHere
```

## Features

### Saved locations support
<img height="508" alt="image" src="https://github.com/user-attachments/assets/86b08d29-6009-4662-86ba-5fbd57ed95bd" />

### Multimodal routing with route preview
<img height="508" alt="image" src="https://github.com/user-attachments/assets/348eb467-e855-45e6-bda1-ffac07360585" />

### Map- and list-based navigation
<img height="508" alt="image" src="https://github.com/user-attachments/assets/769cd377-7062-4db8-b0b6-347f227266e8" />

### Custom arrival and departure times
<img height="508" alt="image" src="https://github.com/user-attachments/assets/8b458e4f-3738-49bf-9c84-430ec2518b26" />

## Future plans

I plan to contact TransitAPI to at least get an idea of pricing and see if it is feasible.

I also am considering adding support for the Transitland API (which has a more generous free tier) or self-hosting a Transitland instance, though I do not have much time or bandwidth.
