
# Developer notes

## New on dev branch
- Giant refactor of file organization
- Transitous routing integration
- Attributions screen + proper User-Agent for requests
- Redesign GTFS manager screen
    - Add overview screen showing total space used and count of saved sources
    - Allow adding multiple consecutive individual GTFS sources, instead of immediately exiting Browse view upon adding a source.
        - Also redesign to allow parallel downloads when doing this.
- Add default location setting and wire into routing and nearby stops/routes
- Add initialZoom param to map, and increase default zoom for nearby stops/routes
- Improve map readability by upscaling tiles
- Add support for toggling map color

## In progress
