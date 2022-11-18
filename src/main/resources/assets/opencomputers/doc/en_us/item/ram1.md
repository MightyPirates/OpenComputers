# Memory

![Do you remember, dancing in September~](oredict:opencomputers:ram1)

Memory is, like the [CPU](cpu1.md), an essential part in all [computers](../general/computer.md). Depending on the [CPU](cpu1.md)'s architecture, the memory has a very essential effect on what a [computer](../general/computer.md) can and cannot do. For the standard Lua architecture, for example, it controls the actual amount of memory Lua scripts can use. This means that to run larger and more memory-intensive programs, you will need more memory.

RAM is available in multiple tiers with the following capacities, by default:
- Tier 1: 192KB
- Tier 1.5: 256KB 
- Tier 2: 384KB
- Tier 2.5: 512KB
- Tier 3: 768KB
- Tier 3.5: 1024KB

Note that these values only apply to the Lua architecture. Other architectures may provide different amounts of memory for the different tiers. Also note that tier 1 and 1.5 memory are both considered tier 1 memory, and similarly for tier 2 and 3 memory. 

The values can be changed in the configuration, if so desired.
