package com.badgerson.larion;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.levelgen.SurfaceRules;

public enum SomewhatSteepMaterialCondition implements SurfaceRules.ConditionSource{
  INSTANCE;

  static final MapCodec<SomewhatSteepMaterialCondition> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public MapCodec<? extends SurfaceRules.ConditionSource> codec() {
    return CODEC;
  }

  public SurfaceRules.Condition apply(SurfaceRules.Context context) {
    return ((MaterialRuleContextExtensions)(Object)context).getSomewhatSteepSlopePredicate();
  }
}
