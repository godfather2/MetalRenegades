/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.metalrenegades.ai.system;

import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.metalrenegades.ai.component.ResidentComponent;
import org.terasology.metalrenegades.ai.component.HomeComponent;
import org.terasology.metalrenegades.ai.component.PotentialHomeComponent;
import org.terasology.registry.In;

import java.util.Collection;

/**
 * Spawns new residents inside of available buildings with {@link PotentialHomeComponent}.
 */
@RegisterSystem(value = RegisterMode.AUTHORITY)
public class ResidentSpawnSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final int SPAWN_CHECK_DELAY = 30;
    private static final int VERTICAL_SPAWN_OFFSET = 2;

    private float spawnTimer;

    @In
    private EntityManager entityManager;

    @In
    private PrefabManager prefabManager;

    @Override
    public void update(float delta) {
        spawnTimer += delta;

        if (spawnTimer > SPAWN_CHECK_DELAY) {
            for (EntityRef entity : entityManager.getEntitiesWith(PotentialHomeComponent.class)) {
                PotentialHomeComponent potentialHomeComponent = entity.getComponent(PotentialHomeComponent.class);
                if (potentialHomeComponent.residents.size() >= potentialHomeComponent.maxResidents) {
                    continue;
                }

                EntityRef resident = spawnResident(entity);
                if (resident == null) { // if no entity was generated.
                    continue;
                }

                potentialHomeComponent.residents.add(resident);

                entity.saveComponent(potentialHomeComponent);
            }

            spawnTimer = 0;
        }
    }

    /**
     * Spawns a random resident inside the center of a provided building entity.
     *
     * @param homeEntity The building entity to spawn inside.
     * @return The new resident entity, or null if spawning is not possible.
     */
    private EntityRef spawnResident(EntityRef homeEntity) {
        Prefab residentPrefab = chooseResidentPrefab();
        if (residentPrefab == null) { // if no prefab is available.
            return null;
        }

        EntityBuilder entityBuilder = entityManager.newBuilder(chooseResidentPrefab());

        LocationComponent homeLocationComponent = homeEntity.getComponent(LocationComponent.class);
        LocationComponent residentLocationComponent = entityBuilder.getComponent(LocationComponent.class);
        HomeComponent homeComponent = new HomeComponent();

        homeComponent.building = homeEntity;
        residentLocationComponent.setWorldPosition(homeLocationComponent.getWorldPosition().addY(VERTICAL_SPAWN_OFFSET));

        entityBuilder.addComponent(homeComponent);
        entityBuilder.saveComponent(residentLocationComponent);

        return entityBuilder.build();
    }

    /**
     * Selects a random resident prefab from a collection of prefabs with {@link ResidentComponent}.
     *
     * @return A random resident prefab, or null if none are available.
     */
    private Prefab chooseResidentPrefab() {
        Collection<Prefab> residentList = prefabManager.listPrefabs(ResidentComponent.class);

        int i = (int) (Math.random() * residentList.size());
        for (Prefab prefab: residentList) {
            if (i-- <= 0) {
                return prefab;
            }
        }
        return null;
    }
}