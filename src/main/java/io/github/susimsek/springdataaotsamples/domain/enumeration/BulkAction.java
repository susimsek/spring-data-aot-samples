package io.github.susimsek.springdataaotsamples.domain.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                """
                Bulk action:
                 * `DELETE_SOFT` - Soft delete (move to trash)
                 * `RESTORE` - Restore from trash
                 * `DELETE_FOREVER` - Permanently delete\
                """,
        enumAsRef = true)
public enum BulkAction {
    DELETE_SOFT,
    RESTORE,
    DELETE_FOREVER
}
