package org.sentilo.web.catalog.admin.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sentilo.common.domain.CatalogComponent;
import org.sentilo.common.domain.CatalogSensor;
import org.sentilo.common.domain.MutableCatalogElement;
import org.sentilo.web.catalog.domain.CatalogDocument;
import org.sentilo.web.catalog.domain.Component;
import org.sentilo.web.catalog.domain.FederationConfig;
import org.sentilo.web.catalog.domain.Sensor;

/**
 * Support class used to calculate and hold the difference between two collections of catalog
 * resources: remote and local resources. The delta process returns remote resources that don't
 * exist into local set, resources that exist but have been updated and local resources that don't
 * exist into remote set and therefore should be deleted.
 *
 * @param <S> The type of the remote catalog resources (either {@link CatalogSensor} or
 *        {@link CatalogComponent})
 * @param <T> The type of the local catalog resources (either {@link Sensor} or {@link Component})
 */
public class FederatedResourcesDelta<S extends MutableCatalogElement, T extends CatalogDocument> {

  private List<String> resourcesToInsert;
  private List<String> resourcesToUpdate;
  private List<String> resourcesToDelete;

  private final Map<String, S> remoteResources;
  private final Map<String, T> localResources;
  private final FederationConfig fConfig;

  private FederatedResourcesDelta(final FederationConfig fConfig, final Map<String, S> remoteResources, final Map<String, T> localResources) {
    this.remoteResources = remoteResources;
    this.localResources = localResources;
    this.fConfig = fConfig;

    calculateDelta();
  }

  private void calculateDelta() {

    final long remoteLastSyncTime = fConfig.getLastSyncTime() != null ? fConfig.getLastSyncTime().getTime() : 0;

    resourcesToInsert = new ArrayList<String>();
    resourcesToUpdate = new ArrayList<String>();
    resourcesToDelete = new ArrayList<String>();

    // First iteration found resources to insert or update:
    // - Each resource that exists into remote set and not into local set is marked as a resource to
    // insert
    // - If a remote resource exists in both, remote and local sets, but its updated time is greater
    // than the
    // last time of synchronization, then it is marked as a resource to update
    for (final String remoteResourceId : remoteResources.keySet()) {
      if (!localResources.containsKey(remoteResourceId)) {
        resourcesToInsert.add(remoteResourceId);
      } else {
        final S remoteResource = remoteResources.get(remoteResourceId);
        if (remoteResource.getUpdatedAt() > remoteLastSyncTime) {
          resourcesToUpdate.add(remoteResourceId);
        }
      }
    }

    // Second iteration found resources to delete, i.e., the same policy followed to detect
    // resources to
    // insert but
    // reversing the order of the sets
    for (final String localResourceId : localResources.keySet()) {
      if (!remoteResources.containsKey(localResourceId)) {
        resourcesToDelete.add(localResourceId);
      }
    }
  }

  public List<String> getResourcesToInsert() {
    return resourcesToInsert;
  }

  public List<String> getResourcesToUpdate() {
    return resourcesToUpdate;
  }

  public List<String> getResourcesToDelete() {
    return resourcesToDelete;
  }

  public Map<String, T> getLocalResources() {
    return localResources;
  }

  public Map<String, S> getRemoteResources() {
    return remoteResources;
  }

  public static <S extends MutableCatalogElement, T extends CatalogDocument> FederatedResourcesDelta<S, T> build(final FederationConfig fConfig,
      final Map<String, S> remoteResources, final Map<String, T> localResources) {
    return new FederatedResourcesDelta<S, T>(fConfig, remoteResources, localResources);
  }

}
