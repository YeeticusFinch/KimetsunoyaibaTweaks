**Colors**

The normal thunder breathing colors are yellow and light blue
The blue variant of thunder breathing (for the blue thunder breathing swords) are dark blue and light blue
For Kaigaku's sword (human), the colors are the same as the blue variant sword (dark blue and light blue)
For Kaigaku's sword (demon), the colors are dark blue and black

Normal Swords (demon slayers):

- Primary Color: Yellow
- Secondary Color: Light Blue

Blue Variant Swords (demon slayers) and Kaigaku's Sword (human):

- Primary Color: Dark Blue
- Secondary Color: Light Blue

Normal Swords (demons):

- Primary Color: Yellow
- Secondary Color: Black

Blue Variant Swords (demons) and Kaigaku's Sword (demon):

- Primary Color: Dark Blue
- Secondary Color: Black

**Swords**

Normal Sword
- Has forms 1-6

Blue Variant Sword:
- Has forms 1-6

Kaigaku's Sword (human)
- Has forms 2-6

Zenitsu's Sword
- Has form 1, and all the variations for form 1 (sixfold, eightfold, and godspeed)
- Has form 7

Kaigaku's Sword (demon)
- Has forms 2-6 (with the kaigaku demon modifications, blue and black colors, and electrocuted effect)
- Has the hashira sword damage instead of the regular sword damage

Hashira Sword
- Has forms 1-7
- Has the hashira sword damage instead of the regular sword damage

Zenitsu's Sword (Full Potential)
- Has forms 1-7, and all the variations for form 1 (sixfold, eightfold, and godspeed) as actual variations rather than independent forms (meaning you use the variation cycler key on form 1 to access the variations)
- Has the hashira sword damage instead of the regular sword damage


**Lightning Bolts**

Thunder breathing relies on lightning bolts for visual effects
These lightning bolts are similar to the vanilla ones, except they have a start position and an end position, and they have multiple colors
A lightning bolt is a square beam that is one block wide. It is drawn as multiple beams super imposed on each-other.

- The outer beam is a full block wide, and it has 20% opacity
- Then there is another beam that is 0.8 blocks wide with 50% opacity
- Then another beam that is 0.6 blocks wide with 75% opacity
- Then a final inner beam that is 0.5 blocks wide with 100% opacity (this one is always white)

All 3 outer beams (the 20% opacity, 50%, and 75%) all have the same color, they use a gradient color, shifting between primary color and secondary color, with primary color being at the start location and secondary color being at the end location

- For the normal swords (the ones that are yellow and light blue), it should shift to white in the middle of the gradient instead of green, so the gradient should go yellow -> white -> lightb-lue

The beams are glowing too.

When a lightning bolt spawns, it is given an array of positions that define its path, with the first item being the start position and the last item being the end position, and it's given a lifetime too (default lifetime 15 ticks)

- During its lifetime, more positions can be appended to the lightning bolt, and the existing positions can be modified

**Lightning Sparks**

Lightning Sparks are similar to lightning bolts. However they are simpler and usually smaller.

Lightning Sparks don't take an array of positions, they just take a start position and an end position as inputs, along with a width (in blocks). The width is a float, so it can be less than a block. The inputted width refers to the width of the outer beam, and then the inner beam is half that width. It also takes in two colors, an outer color (default light blue) and an inner color (default white). The Lightning Spark is also given a tick lifetime as an input. Also, the start position and end position of the lightning sparks should be changeable. And it should also be possible to give the Lightning Sparks a velocity vector (the velocity vector gets added to both the start position and the end position on every tick).

Lightning Sparks also take a flickering boolean. If flickering is true, then they only render every other tick, so visible for 1 tick, not visible for 1 tick, visible for 1 tick, not visible for 1 tick.

Lightning Sparks also takes in an optional arcing vector. The arcing vector (default 0 0 0) provides both a direction and magnitude to arc the spark segments.

Lightning sparks are just two beams, an outer beam and an inner beam of half size.

- The outer beam is 50% opacity
- The inner beam is 100% opacity
- Both beams are glowing

A Lightning Spark can have multiple segments depending on its length and width. If the length squared of the segment (length squared because it's distance between start position and end position, and square roots are expensive, so just multipy the coefficients by eachother to avoid doing any power functions), if the length squared of the segment is less than 25x the width, then there will only be one segment. If the length squared is 25x the width, then there will be two segments. So ergo the number of segments equals the length squared divided by 25 multiplied by the width (length squared is in numerator, the width multiplying 25 is in the denominator). There will be a minimum of 1 segment, and a maximum of 50 segments (let's guard for divide by 0 errors).

If there is a non-zero arcing vector, then each of the segments will arc in the direction of that arcing vector, depending on proximity to middle. So like the edges (start position and end position) aren't affected at all. The middle position is shifted by 100% of the arc vector, so for that position in the middle of the segments, the arc vector should be added directly to it. For the position at 25% through the segments, and 75% through the segments, half of the arc vector should be added.

The first segment starts at the start position of the Lightning Spark. The last segment ends at the end position of the Lightning Spark. The positions of all the other points for the segments are calculated every other tick (so half are calculated on one tick, the other half are calculated on the other tick). These intermediate point locations are just calculated locally, like in rendering, no need to sync this with server, as it's just graphics. They are calculated by arranging the points sequentially in a straight line between the start position and end position, and then adding a random offset to every point in a random direction, the offset magnitude being a maximum of 5\*width.

## Electrocuted Effect (Kaigaku Effect)

When the user is using Kaigaku's sword, any entities that take damage from one of the forms will get the Electrocuted Effect

Turns your hearts into the electrocuted hearts textures

Plays the kaigaku_effect sound on loop

## First form: Thunderclap and Flash

The user holds the iai1 animation (the iai1 animation is the unsheathing pose) then dashes forward at great speed, must aim properly, performing a single strong slash to any opponent in their path.
The attack goes right through them, they simply take the damage if they're in the way.
The trajectory is marked by our custom lightning bolt, with the start position being 10 blocks behind the location where the form was activated (opposite the user's look direction), and the end position being the target location.

Tick 0
- Play the thunder_chargeup sound

Ticks 0-19

- Holding the iai1 animation
- Staying absolutely still, can't move around (but can still rotate)
- If the user isn't a player, then the user will be always looking directly at their target

Tick 20

- Raycast 20 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
- Save that target location at the end of the raycast

Tick 21

- A lightning bolt is created, with the start point being the starting location, and the end point being the user's current location
- User plays the `sword_to_right` animation
- Play a loud thunderclap sound on the user, and for all players within 30 blocks

Ticks 21-25

- User is launched to the target location, their velocity fixed such that they reach the target location by tick 25
- The lightning bolt's end position gets updated every tick to be the user's location

Tick 25

- Do some flash particles with some spark particles
- The lightning bolt's end position gets permanently set to the target location
- All valid target entities within 2 blocks of the trajectory (between the start location and target location) get damaged and get flash particles on them
- Create 4 Lightning Sparks, with the start location being the center of the user (halfway between the user's position and head position, accounting for local gravity direction), the end location being a random solid block within 2 blocks (or if no solid blocks are near, then just a random point 2 blocks away and no arcing), and the arcing vector being [0, 1, 0]. They should use an outer width of 0.0875 blocks, flickering=true, and last 8 ticks lifetime duration. Their color is secondary color.

Ticks 26-36

- User plays the unsheathing animation
- User is frozen

Tick 37

- Cleanup, user is unfrozen, animation stops

## First form (variation): Thunderclap and Flash, Sixfold

The user holds the pose a little longer, then dashes forward at great speed to wherever he was aiming, they will land completely still with the pose and must press (m1/m2) to immediately do it again, if there is a single target, it's recommended to aim, click, aim, click, aim, click, as fast as you can, mastering this means super quick attacks from many directions, but it is difficult to master the aim. (However if there are are targets all over the place, have fun happy-clicking)
For one, you might miss your target in one or two dashes. For two, if you take too long (about 2 seconds) aiming, the flow of the attack breaks and it simply stops, leaving you with the cooldown

The trajectory is marked by our custom lightning bolt, with the start position being 10 blocks behind the location where the form was activated (opposite the user's look direction), and the end position being the target location, and all the locations in-between being all the click target locations

Tick 0
- Play the thunder_chargeup_sixfold sound

Ticks 0-29

- Holding the iai1 animation
- Staying absolutely still, can't move around (but can still rotate)
- If the user isn't a player, then the user will be always looking directly at their target

Tick 30

- A lightning bolt is created, with the start point being the starting location, and the end point being the user's current location
- This lightning bolt has a lifetime of 40 ticks

Tick 30-70

- During this 40 tick period, every time the user right clicks or left clicks (either clicking an entity, the air, or a block), it does the launch sequence
- In this move, the user can do up to 6 launch sequences
- Each click launches up to 15 blocks, and a launch sequence can't execute while one is already in progress. Clicks during an active launch are queued and launch as soon as the current launch ends. Each launch sequence lasts 5 ticks
- As soon as the 6th launch sequence has completed, it will break to the ending clause, ending the 40 ticks early
- Once these 40 ticks have expired (going past tick 65), it will go to the ending clause

Launch:
first tick of launch sequence (do this once per launch)

- Raycast 15 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
- Save that target location at the end of the raycast
- User plays the `sword_to_right` animation
  Ticks 1-5 of launch sequence (do this every tick)
- User is launched to the target location, their velocity fixed such that they reach the target location by tick 5 of the launch sequence
- The lightning bolt's end position gets updated every tick to be the user's location
  Tick 5 of launch sequence (do this once per launch, at the end of the launch)
- Do some flash particles with some spark particles at the target location
- In the array of positions in the lightning bolt's path, set the last position to the target position
- And then append another position to that array, being the player's position, that new appended position will be the new end position, and this will be the end position that will be updated in the next launch
- All valid target entities within 2 blocks of this launch trajectory get damaged and get flash particles on them
- Play a loud thunderclap sound on the user, and for all players within 30 blocks

Ending - 20 ticks

- User plays the unsheathing animation
- User is frozen
- Create 4 Lightning Sparks, with the start location being the center of the user (halfway between the user's position and head position, accounting for local gravity direction), the end location being a random solid block within 2 blocks (or if no solid blocks are near, then just a random point 2 blocks away and no arcing), and the arcing vector being [0, 1, 0]. They should use an outer width of 0.0875 blocks, flickering=true, and last 8 ticks lifetime duration. Their color is secondary color.

Cleanup - 20 ticks after ending

- Cleanup, user is unfrozen, animation stops

## First form (variation): Thunderclap and Flash, Eightfold

The user holds the pose yet a little longer, then dashes forward at great speed to where the crosshair was, the mechanic is the same as with sixfold, except of course, this one has two extra hits, longer initial pose and slightly longer cooldown.

The trajectory is marked by our custom lightning bolt, with the start position being 10 blocks behind the location where the form was activated (opposite the user's look direction), and the end position being the target location, and all the locations in-between being all the click target locations

Tick 0
- Play the thunder_chargeup_eightfold sound

Ticks 0-39

- Holding the unsheathing animation
- Staying absolutely still, can't move around (but can still rotate)
- If the user isn't a player, then the user will be always looking directly at their target

Tick 40

- A lightning bolt is created, with the start point being the starting location, and the end point being the user's current location
- This lightning bolt has a lifetime of 40 ticks

Tick 40-100

- During this 60 tick period, every time the user right clicks or left clicks (either clicking an entity, the air, or a block), it does the launch sequence
- In this move, the user can do up to 8 launch sequences
- Each click launches up to 10 blocks, and a launch sequence can't execute while one is already in progress. Clicks during an active launch are queued and launch as soon as the current launch ends. Each launch sequence lasts 3 ticks
- As soon as the 8th launch sequence has completed, it will break to the ending clause, ending the 60 ticks early
- Once these 60 ticks have expired (going past tick 65), it will go to the ending clause

Launch:
first tick of launch sequence (do this once per launch)

- Raycast 10 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
- Save that target location at the end of the raycast
- User plays the `sword_to_right` animation
  Ticks 1-3 of launch sequence (do this every tick)
- User is launched to the target location, their velocity fixed such that they reach the target location by tick 3 of the launch sequence
- The lightning bolt's end position gets updated every tick to be the user's location
  Tick 3 of launch sequence (do this once per launch, at the end of the launch)
- Do some flash particles with some spark particles at the target location
- In the array of positions in the lightning bolt's path, set the last position to the target position
- And then append another position to that array, being the player's position, that new appended position will be the new end position, and this will be the end position that will be updated in the next launch
- All valid target entities within 2 blocks of this launch trajectory get damaged and get flash particles on them
- Play a loud thunderclap sound on the user, and for all players within 30 blocks

Ending - 15 ticks

- User plays the unsheathing animation
- User is frozen
- Create 4 Lightning Sparks, with the start location being the center of the user (halfway between the user's position and head position, accounting for local gravity direction), the end location being a random solid block within 2 blocks (or if no solid blocks are near, then just a random point 2 blocks away and no arcing), and the arcing vector being [0, 1, 0]. They should use an outer width of 0.0875 blocks, flickering=true, and last 8 ticks lifetime duration. Their color is secondary color.

Cleanup - 15 ticks after ending

- Cleanup, user is unfrozen, animation stops

## First form (variation): Thunderclap and Flash, Godspeed

Longer pose, but thicker hitbox, greater distance, overall greater speed, nearly tp speed, but same damage, however it has the longest cooldown among these variations. (the sound of totem of undying plays)
Downside: When dashing you take some damage, at least 3 hearts due to the great force used on your legs. 

Tick 0
- Play the thunder_chargeup_godspeed sound
- Create 5 Lightning Sparks, with the start location being the center of the user (halfway between the user's position and head position, accounting for local gravity direction), the end location being a random solid block within 2 blocks (or if no solid blocks are near, then just a random point 2 blocks away and no arcing), and the arcing vector being [0, 1, 0]. They should use an outer width of 0.0875 blocks, flickering=false, and last 50 ticks lifetime duration, and the color should be primary color

Ticks 0-49

- Holding the iai1 animation
- Staying absolutely still, can't move around (but can still rotate)
- If the user isn't a player, then the user will be always looking directly at their target

Tick 50

- Raycast 60 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
- Save that target location at the end of the raycast

Tick 51

- A lightning bolt is created, with the start point being 10 blocks behind the user's starting location, and the end point being the target location
- Create 3 lightning sparks, with the start point being 10 blocks behind the user's starting location, and the end point being the target location, the color being primary color, and the outer width being 0.5 blocks
- User plays the `speed_attack_sword` animation
- Play a loud thunder_godspeed sound on the user, and for all players within 100 blocks
- Teleport the user to the target location  (to the nearest unobstructed safe location that isn't underground or inside a block)
- Flash particles and spark particles on the target location
- User takes 6 points of damage
- All entities within 6 blocks of the line of the form (between start location and end location) take damage
- All entities within 12 blocks of the target location take the damage and get a primary color lightning spark going from their center (halfway between their location and their head location) to 1 block above the target location of the form (local gravity aware)
- Create 5 lightning sparks at the target location, their start position being set to 1 block above the target location (local gravity aware), and their end position being set to a random location 12 blocks away, color being primary color
- Every entity that gets damaged will get a flash particle on them

Ticks 52-77

- User plays the unsheathing animation
- User is frozen

Tick 78

- Cleanup, user is unfrozen, animation stops


## Second Form: Rice Spirit

The user dashes, seeminlgy going right through them, but in reality user delivers five very fast cuts, the opponent it's seemingly paralyzed after the user went past them, then they receive the five hit damage, the last one has great knockback, the visual is as if the opponent is electrified then receives a final shock that sends them flying. (Similar to love breathing first form except this only hits one target, has greater damage, paralysis, and extra knockback)

rice_spirit_dash

rice_spirit_electric_damage

rice_spirit_knockback

## Third Form: Thunder Swarm

(Lock on) The user dashes right past the opponent, twisting to slash them from behind, this is the first of many, the user retries, essentially teleporting behind the opponent again, slash, tp behind again, slash, over and over, this becomes worse as user's afterimage doubles the attack, so it always looks like the user is attacking the back while the after image attacks the front, and viceversa. These slashes are not very strong, but this form is pretty much impossible to be completely parried or dodged.

thunder_swarm_dash

thunder_swarm_after_image_swarm

## Fourth Form: Distant Thunder

The user dashes forward at mid speed while performing multiple quick slashes, almost drawing a pound sign (#), lightning and thunder visuals appear, small at first, then a bigger electrocuting shows up only a second after the cuts, that's when the main damage hits. Visually it looks like zaps of thunder following the sword slashes at every slash, making an electric sound.
Option 2: User makes the "Pound #" slashes where he stands, each slash sends a lightning ray forward, these rays flash and blind the opponent and do no damage, but as the opponent is flashed the user dashes towards the opponent and slashes them once, causing the real damage, with a thunder sound. Just like a distant thunder, first you're blinded by the lihgtning then the thunder arrives.


distant_thunder_blinding_slashes

distant_thunder_dash

distant_thunder_slash

## Fifth Form: Heat Lightning

Hold pose as if charging power, red zaps all over
The user dashes forward delivering an upwards very powerful slash, which leaves the opponent paralyzed and receiving burning damage even after hit, irradiating those red zaps, the damage is similar than that of stepping in fire.


heat_lightning_charging

heat_lightning_takeoff

heat_lightning_heat_slash

heat_lightning_paralyzing_damage

## Sixth Form: Rumble and Flahs

The user holds a pose as black smoke starts to envelop them, then the user dashes forward enveloped in a cloud of dark smoke, delivering a single very strong, explosive sideways cut, 360° spin while cutting (similar to "rings of ember"), small lightnings appear on the cloud, then big ones irradiate from the sword strike, with higher range, the hitbox extends forward, the sound is like an explosion of thunder.

rumble_and_flash_charging

rumble_and_flash_takeoff

rumble_and_flash_strong_slash

rumble_and_flash_leftover_lightning

## Seventh Form: Flaming Thunder God

Charging pose, huge lightning irradiates from user, slow, growing, silent, here the user should aim carefully, the user then travels great distance at tremendous speed, with a single sideways cut delivered, enormous damage, one shot for not very strong opponents, big cooldown, can be performed wherever the user is aiming, either horizontally or vertically.
Visually a flaming, yellow huge long dragon of thunder follows the user's trajectory, the after image of that dragon stays for a couple seconds after the hit was delivered (just to be appreciated, for due to the tremendous speed of the user, youll hardly be able to see it in movement). Downside: The user is paralyzed a few seconds after performing the attack, due to the great power output
This form is ideal for an opponent at big distance, the closer you are, the more chance they'll have to get out of your corsshair and dodge it, the further away, the better chance you have, for you must aim with your crosshair, and the hitbox of the slash is wide, but if the attack misses, it leaves you sold...


thunder_god_charging

thunder_god_ultimate_slash



- Cleanup, user is unfrozen, animation stops
