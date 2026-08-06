
# Light Transit

A transit app for the Light Phone III.

## What's the difference between this and PicoTransit?

tl;dr you probably want PicoTransit.

This uses the [Transit API](https://transitapp.com/partners/apis), which offers neat features like advanced multimodal routing, bikeshare availability, live updates, and support for 1200+ cities out of the box. *However,* the API is not free for production use. A developer API key is free with limited usage (adequate enough for only one user), and must be formally requested from Transit (see link above).

I do plan to contact Transit to at least get an idea of pricing.

PicoTransit's focus is less on routing (as of right now), and it does not use an API; instead it implements support for transit agencies individually (free).
