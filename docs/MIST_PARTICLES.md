# Mist Particle System

## Overview
The Kimetsunoyaiba Multiplayer mod includes a custom mist particle system that creates realistic mist effects with floating behavior, random texture selection, and settling near the ground.

## Features

### Texture Variety
- Two mist textures are used: `mist_particle_large.png` and `mist_particle_small.png`
- Particles randomly choose between the two textures when spawned
- Once spawned, particles keep their texture throughout their entire lifetime for consistency

### Movement Behavior
- **Random Slow Movement**: Particles move slowly in random horizontal directions
- **Gentle Descent**: Particles slowly descend simulating gravity
- **Buoyancy Effects**: Occasionally particles move upward slightly to simulate natural mist behavior
- **Drag Simulation**: Horizontal movement gradually slows down over time

### Visual Properties
- **Lifetime**: 10-15 seconds (200-300 game ticks)
- **Size**: Large particles with random size between 0.5 and 3.0 units (10-30x bigger than standard particles)
- **Transparency**: Particles are semi-transparent (alpha 0.7-1.0)
- **Color**: Light blue/white color with slight random variations

## Technical Implementation

### Classes
- `MistParticle.java`: Core particle implementation with behavior logic
- `MistParticle.Provider`: Handles texture registration and particle creation
- `ModParticles.java`: Particle type registration
- `MistParticleHandler.java`: Utility methods for spawning particles

### Resource Files
- `assets/kimetsunoyaibamultiplayer/textures/particle/mist_particle_large.png`
- `assets/kimetsunoyaibamultiplayer/textures/particle/mist_particle_small.png`
- `assets/kimetsunoyaibamultiplayer/particles/mist.json`

## Usage

### Spawning Mist Particles
The mod provides utility methods for spawning mist particles:

```java
// Spawn a single mist particle at a location
MistParticleHandler.spawnMistParticle(x, y, z);

// Spawn multiple particles in an area
MistParticleHandler.spawnMistParticles(x, y, z, count, spread);

// Spawn with specific velocity
MistParticleHandler.spawnMistParticleWithVelocity(position, velocity);
```

### Particle Registration
The particle is registered as `kimetsunoyaibamultiplayer:mist` and can be used in commands or other systems.

## Customization

### Adding to Other Systems
To integrate mist particles into your own features:
1. Import the particle type: `ModParticles.MIST_PARTICLE.get()`
2. Use Minecraft's particle spawning system with the mist particle
3. Adjust velocity parameters for the desired movement pattern

### Texture Requirements
When creating additional mist textures:
- Use a square PNG format
- Size should be a power of 2 (e.g., 16x16, 32x32)
- Maintain alpha transparency for proper blending
- Use light colors for the best mist effect