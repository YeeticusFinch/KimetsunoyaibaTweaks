# Biokineses

Muzan's blood demon art is called "Biokineses". This is a legendary blood demon art.

**Brambles**

Brambles are similar to Lightning Sparks. However they do not glow, and they are just the outer beam, no inner beams, and no glowing either.

Brambles don't take an array of positions, they just take a start position and an end position as inputs, along with a width (in blocks). The width is a float, so it can be less than a block. The inputted width refers to the width of the outer segment (there is no inner segment, unlike the Lightning Sparks). It also takes in two colors (default red and black). The Bramble is also given a tick lifetime as an input. Also, the start position and end position of the brambles should be changeable. And it should also be possible to give the Brambles a velocity vector (the velocity vector gets added to both the start position and the end position on every tick).

Brambles also takes in an optional arcing vector. The arcing vector (default 0 0 0) provides both a direction and magnitude to arc the spark segments.

A Bramble is just one beam with 100% opacity and no glow

- The beam is two colors, split lengthwise down the middle

A Bramble can have multiple segments depending on its length and width. If the length squared of the segment (length squared because it's distance between start position and end position, and square roots are expensive, so just multipy the coefficients by eachother to avoid doing any power functions), if the length squared of the segment is less than 25x the width, then there will only be one segment. If the length squared is 25x the width, then there will be two segments. So ergo the number of segments equals the length squared divided by 25 multiplied by the width (length squared is in numerator, the width multiplying 25 is in the denominator). There will be a minimum of 1 segment, and a maximum of 50 segments (let's guard for divide by 0 errors).

If there is a non-zero arcing vector, then each of the segments will arc in the direction of that arcing vector, depending on proximity to middle. So like the edges (start position and end position) aren't affected at all. The middle position is shifted by 100% of the arc vector, so for that position in the middle of the segments, the arc vector should be added directly to it. For the position at 25% through the segments, and 75% through the segments, half of the arc vector should be added.

The first segment starts at the start position of the Bramble. The last segment ends at the end position of the Bramble. The positions of all the other points for the segments are calculated every other tick (so half are calculated on one tick, the other half are calculated on the other tick). These intermediate point locations are just calculated locally, like in rendering, no need to sync this with server, as it's just graphics. They are calculated by arranging the points sequentially in a straight line between the start position and end position, and then adding a random offset to every point in a random direction, the offset magnitude being a maximum of 5\*width.

Between each segment, like at the joint between segments, the segments should overlap eachother slightly, each segment should go slightly past the joint

**Blood Tornado**

- The blood tornado is an entity/model, it's an asset that resembles a spinning red and black tornado
- It deals damage to all nearby entities, and it catches nearby projectiles

**Muzan hands**

- The biokineses_0 item (and the biokineses_0l rendered in the left hand, if the ability calls for it)

**Arm Blade**

- The biokineses_1 item (and the biomineses_1l rendered in the left hand, if the ability calls for it)
- this is an animated Geckolib item, it uses the kanroji sword animation file, so the same animations as the kanroji sword, with a similar handler, because it's basically a kanroji sword with a different model (but same bone groups)

**Flesh Vines**

- A thick rendered tentacle, similar to the back tentacles, but much thicker, with the flesh vine texture
- the flesh_vine_tip model is rendered at the end of the flesh vine, facing the direction that the end is facing
- the start of the flesh vine can either be connected to a location (which can move), it it can be connected to one of the bones of the user (like the torso or the arm, similar to the back tentacles)
- the flesh vine should have a little bit of randomness in it's path, and it should be quite lumpy, the segments varying in thickness somewhat randomly, with some lumps and protrusions here and there, and they should be wriggling and writhing
- The flesh vines will also need an implementation to handle a custom defined path, with an array of points to define that path (like for the sinusoid motion of ultimate authority)

## Black Blood, Brambles

_Muzan hands_

Muzan lifts and aims his left forearm towards his target, before uses his blood from his veins, that creates a red-and-black veins-like cracks, to create an array of thorny, thin tendrils to entangle and slash his target.

Tick 0

- Biokineses item uses the biokineses_0 model (and renders biokineses_0l on the left hand)
- Play the brambles animation (lasts until tick 40)

Ticks 0-17 (chargeup phase)

- User can only move at 0.25x speed
- Charging up the blood brambles
- Tiny black and red swirling dust particles (size 0.1) centered at the user's head position shifted 0.5 blocks forward and 0.4 blocks to the left (from the user's perspective, local gravity aware)

Tick 18

- Create 5 brambles, red and black in color, and 0.05 to 0.1 in width (random between those)
  - 1 bramble is black only, 1 bramble is red only, and the rest of both red and black

Ticks 18-60

- Set brambleStartPos to the user's head position shifted 0.8 blocks forward and 0.3 blocks to the left (from the user's perspective, local gravity aware),
- Each bramble has the start position set to brambleStartPos shifted by a random vector of magnitude up to 0.06 (updated every tick)
- Every tick, increment brambleRange by 6 blocks, up to a maximum of 60 blocks
- Each bramble will have a different randomized end position, to calculate that position, take the start position, add the user's forward vector multiplied by brambleRange, and then shift it by a random vector in a random direction of a magnitude up to a maximum of a third of brambleRange
- That randomized end position will be recalculated every tick (so no point in storing them)
- Each of the brambles should get a random arcing vector of magnitude up to a maximum of a fifth of brambleRange (recalculated every tick)
- Place a blackred_slash particle at the end position of every bramble
- All living entities in that cone (length is brambleRange, radius is a third of brambleRange) will take damage every 2 ticks (and yes that should ignore/reset the damage cooldowns, using our Damager.hurt function), also whenever a living entity is damaged by this ability, it should get some blackred_impact particles on it
- Every 2 ticks, raycast forward a full 60 blocks, at every solid block that intersects that raycast, place a blackred_slash

## Merciless Punishment

_Muzan Hands_

_Muzan Hands_

The user swipes his left arm, creating a violent storm of red scratches with his sharp fingernails, and unleashing a blood tornado.

Tick 0 (setup)

- Biokineses item uses the biokineses_0 model (and renders biokineses_0l on the left hand)
- Plays the left_arm_overhead animation with the red claw swing and AOE damage nearby
- Sets up the guard state
- Spawns a blood_tornado entity and is launched forward as a projectile that travels along the ground

Ticks 0-20

- The user moves at half movement speed
- The blood_tornado entity travels along the ground, continuing to move forward 2 blocks per tick

Tick 21 (cleanup)

- Form cleanup, the animation on the user is cancelled, the guard state is removed, the user is no longer slowed
- The blood_tornado entity is removed

## Ultimate Authority

_Muzan Hands_

The user stomps on the ground and a line of upwards flesh vines surges forward, give of them in a line rolling forward like a tidal wave of destruction.

Ticks 0-15 (charge up)

- Biokineses item uses the biokineses_0 model
- play the right_arm_chargeup animation on the user
- User moves at quarter speed and can't jump, user is hovering during this time, 3 blocks higher than where they started the form from (they will float up within the first 5 ticks of the charge up, and this "up" is local gravity aware)
- Create 3 flesh vines on the user's location
- The start location of all 3 flesh vines should be one block below the user's location (local gravity aware), and the end location should be 2 block's above the user (local gravity aware) and a random 1.5 block offset in any direction that is perpendicular to the upward vector (the upward vector in the local gravity)
- The start and end location of all 3 of those flesh vines should be recalculated every tick (with a new random end location each tick)

Tick 17

- User slams downward while playing the heel_drop animation, and the attack happens as soon as the user hits the ground (if the user doesn't hit the ground in the next two seconds, then the ability is cancelled)

When the user hits the ground:

- Calculate a 15 block horizontal line perpendicular to the user's facing direction (7.5 blocks to the left, 7.5 blocks to the right)
- Calculate 5 evenly spaced points along that line (3 blocks apart)
- on each of those 5 points, there will be a flesh vine that spawns, the start location being the nearest solid block underneath that point (do a 1 block offset below that solid block), and the end position being 1 block higher than the starting position, facing vertically

After the user hits the ground and those 5 flesh vines are created

- The 5 vines travel forward in a large sinusoidal path, using a cosine function for the Y position, the vines themselves should take the shape of that sinusoid, as more of the cosine function gets traced out as ticks go on
- The cosine function has an amplitude of 15 blocks
- A damaging AABB box follows the end position of every one of these vines, using the Danaher.hurt function (using the user, the owner of these vines, as the damage source) on all (non-owner) living entities that intersect this AABB box (and they have blackred_impact particles spawned on entities that get damaged, and there will be explosion particles on the ends of these vines whenever the AABB box hits a full block)

Form cleanup:

- Clear the user's animation and guard state

## Black Blood, Descendence

_Muzan Hands_

The user leaps into the air, and then teleports to their target unleashing a ground pound, with red and black brambles shooting out of the ground around the groundpound location.

Ticks 0–20 (Charge-up)

- Biokineses item uses the biokineses_0 model, rendering biokineses_0l on the left hand.
- Play the right_arm_chargeup animation.
- User hovers 20 blocks upwards (and 10 blocks backwards), taking 10 ticks to get there and hovering for 10 ticks
- User movement is reduced to 0.25x speed.
- Spawn small black and red swirling dust particles around the user's body.

Tick 20

- Select the target point (raycast up to 40 blocks from the user's head (in the direction the user is looking), choosing the point at the first intersecting solid block, if no blocks are found, then it will raycast downwards until a solid block is detected)

Ticks 20-25

- The user zooms towards that target location, such that they hit the target location on tick 25
- Play the heel_drop animation

Tick 25

- Deal AOE damage to all living entities within a 6-block radius.
- Apply strong knockback away from the impact point.
- Create an explosion of blackred_impact particles.
- Spawn 10 brambles in a 6 block radius around the user, coming out of the ground, shooting 30 blocks upwards and wriggling around, the tip moving around randomly with a horizontal displacement of up to 6 blocks, and some random arcing vectors of up to 3 blocks in magnitude
- All entities within the path of those brambles take damage and will get the blackred_slash particles on them
- These brambles stay for 30 ticks

Ticks 25-55

- The user is moving at 0.25x movement speed

Tick 56 (cleanup)

- Animations are cleared, guard state is cleared

## Forced Transition

_Flesh arms_

The flesh vines Upon making contact with a convertible mob begins the demonstrating of the entity, with a 50-50 chance of the process killing the target

Tick 0:

- Choose a target entity or location within 80 blocks (80 block raycast forward, select the nearest living entity to the crosshair, or select the block that the raycast intersects, or select the point at the end of the 80 blocks)

Ticks 1-40:

- Play the right_arm_forward animation on loop
- User moves at half movement speed
- A flesh vine with the starting point attached to the user's right arm starts moving forward, towards the target (so like the end position is always facing the target entity/location, and moving towards, slithering through the air randomly to get there)

When the flesh vine hits a target entity, it latches on to the target and stays attached to that target entity for 20 ticks.

- The target entity that the flesh vine is attached to takes 30 damage (using the Damager.hurt with the user / flesh vine owner as the damage source, so it can be scaled) every 10 ticks
- If the user drops to zero HP during this ability from this damage (and they are a demonizeable entity or a player), then there is a 50% chance of instantly turning them into a demon (no demon transformation phase, just instant demon transformation, and if it's a player then it will instantly forcefully feed them 10 to 20 muzan blood, if the player is already a demon then it does nothing, it just lets them die)
- If the user survives this attack, then they get 10 seconds of wither 5, and also slowness 3 for 10 seconds

Tick 50 (cleanup):

- Clear the animation on the user

## Shockwave Energy Blast

The user releases a long-ranged shockwave that severely damages the nervous systems of those caught in it, causing seizures, convulsions, and even rendering them unable to breathe for a brief period of time due to damage inflicted to the target's lungs. He uses it by morphing his torso into a gigantic beastly mouth to release it in all directions.

Tick 0

- Play the shockwave animation on the user
- Play the minecraft:entity.warden.sonic_charge sound effect at 0.75 pitch
- Play the minecraft:entity.guardian.attack sound effect at 0.75 pitch

Ticks 0-12 (chargeup)

- 3 flesh vines will spawn, each one as a circle of radius R, centered on the user's center (halfway between their location and their head location) and circling rapidly around the user's center, with a random orientation that changes every tick
- User moves at 0.25x speed during this chargeup phase
- R starts at 4, and then decreases to 0 by the 12th tick

Tick 13

- Spawn 10 lightning sparks (yes the same ones from thunder breathing), they should each have a width of 0.8, and they should just be entirely white in color
- Play the minecraft:entity.warden.sonic_boom sound effect
- Play the thunder sound effect
- Remove the 3 flesh vines
- R starts at 1

Ticks 13-26

- R increases every tick, reaching 50 by tick 26
- Every tick, spawn a sphere of radius R of flash particles around the user
- For each lightning spark, the start point is set to the user's center location (halfway between their head and location), and the end point is a random point along the edge of the sphere of radius R centered on the user (same sphere as the flash particles)
- For every living entity (other than the user) within R blocks of the user (use the distance squared trick to avoid the square root issue, comparing to R\*R, such that we don't have to use the slow power function), once they first enter the region, they become marked, meaning a new lightning spark will spawn on them (lasting 10 ticks), start point being the user's center position and end point being the living entity's center position (don't keep spawning more lightning sparks on the same living entity over and over again, if a living entity already has a lightning spark from this ability attached to them then just update the start and end positions every tick such that it tracks both entities)
- All living entities who get hit by this ability (they are within R of the user) will take a bunch of damage, and then immediately get slowness 20 for 5 seconds (no particles), and they will have the kimetsunoyaiba lightning particles on them for that duration, and they will also have the suffocation potion effect (from kimetsunoyaiba mod) for 5 seconds, and it will cancel any active abilities or breathing forms they have active (also any entity who has the suffocation effect shouldn't be able to use abilities, they cant use blood demon arts or breathing forms while they have the suffocation effect), and their velocities will also be instantly zeroed when they get hit by this ability

Tick 30 (cleanup)

- Give 5 seconds of slowness 10 to the user (no particles)
- Clear the guard state and the animations on the user

---

## Ignore everything past this line, this is WIP

---

## Blood King’s Massacre

_Tentacles_

Unleash multiple flesh vines in a 50 x 50 area ripping apart and tearing apart opponents, dragging the disemboweled courts back to be consumed by their body for high regeneration, can only be Blocked or negated by hashira or high tier Demon slayers, leaves lasting high-level poison on the survivors, slowly poison their bodies as the fight drags on

## Demon King’s Wrath

_Blade arms, tentacles_

Unleash multiple flesh and bond vines that tear apart the battlefield and also cause aftershock of energy to tear up whatever is left, apart, trees, buildings, and even the hardest of stones nothing is safe besides Hashira who can dodge in time but surviving a hit from this does not mean much because it also inflicts high intensity poison that can once the health of the opponent mobs are low enough either kill or turn them into a demon against their will without being treated but the ability is followed up by consistent whipping slashes!

## Demon King’s Blood Dance

_Tentacles_

The ability selects up to the maximum of 15 mobs before automatically kneeling forward in a dance like motion whipping around multiple bone rips that tear apart the opponent’s bodies dealing high intensity damage in a mockery of a breathing style’s movement leaving the mob to bleed out at the end as they are unable to stop the user from walking off

## Blades of Disaster

_Blade arms_

The user grows long blade-like arms (or flesh vines), crossing them and unleashing them in a large crisscross slice, sending an aftershock of blood tornados that launch forward half a second later that cleaves through everything in its path

## Vengeful Abyss, Fissure

_Tentacles_

The user extends a large fleshy arm in a straight line forwards, mottled with mouths. The mouths suck air inwards, pulling all nearby entities towards the arm. Arm then unleashed bursts of paralyzing energy bolts while the user's back tentacles and leg tentacles surge forward and decimate all the ensnared entities

## Abhorrent Galaxy

The user extends two large fleshy arms mottled with mouths, along with their back tentacles and leg tentacles, rotating them in a large spiral motion. The mouths on the arms are all sucking in air, pulling in nearby entities to be chewed by their hungry gnashing maws while the tentacles whip around and slice everything nearby

## Titanic Globe, Ball of Genocide

The user grows many fleshy arms and appendages, wrapping around themself, forming a large ball of flesh and mouths. The user (in this ball form) rolls forward, crushing everything in its path. Entities who get hit by the ball will get grabbed by a mouth and stay stuck to the ball for the duration of the form, taking damage as they continuously get rolled over (and chewed by the mouth)
While in this form, the user can launch a flesh tentacle (on right click) and swing from it like a wrecking ball

Some random chargeup

- Create 4 brambles on the user's location, red and black in color, and 0.1 width
- The start location of all 4 brambles should be the user's location, and the end location should be 3 block's above the user (local gravity aware) and a random 1 block offset in any direction that is perpendicular to the upward vector (the upward vector in the local gravity)
- The start and end location of all 4 of those brambles should be recalculated every tick (with a new random end location each tick)
