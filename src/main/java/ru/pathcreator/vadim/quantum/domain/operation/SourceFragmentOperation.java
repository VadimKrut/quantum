/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.operation;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.source.ProgramSourceFragment;

/**
 * РћРїРµСЂР°С†РёРѕРЅРЅС‹Р№ С„СЂР°РіРјРµРЅС‚ РёСЃС…РѕРґРЅРѕРіРѕ СЏР·С‹РєР°, РґР»СЏ РєРѕС‚РѕСЂРѕРіРѕ РІ IR РЅРµС‚ СЃС‚СЂСѓРєС‚СѓСЂРЅРѕР№ СЃРµРјР°РЅС‚РёРєРё.
 */
public final class SourceFragmentOperation implements Operation {

    /**
     * РЎРѕС…СЂР°РЅРµРЅРЅС‹Р№ С„СЂР°РіРјРµРЅС‚ РёСЃС…РѕРґРЅРѕРіРѕ СЏР·С‹РєР°.
     */
    private final ProgramSourceFragment fragment;

    /**
     * РЎРѕР·РґР°РµС‚ РѕРїРµСЂР°С†РёСЋ-С„СЂР°РіРјРµРЅС‚.
     *
     * @param fragment С„СЂР°РіРјРµРЅС‚ РёСЃС…РѕРґРЅРѕРіРѕ СЏР·С‹РєР°
     */
    public SourceFragmentOperation(final ProgramSourceFragment fragment) {
        if (fragment == null) {
            throw new IllegalArgumentException("Source fragment must not be null.");
        }
        this.fragment = fragment;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.SOURCE_FRAGMENT;
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ С„СЂР°РіРјРµРЅС‚ РёСЃС…РѕРґРЅРѕРіРѕ СЏР·С‹РєР°.
     *
     * @return С„СЂР°РіРјРµРЅС‚
     */
    public ProgramSourceFragment fragment() {
        return fragment;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SourceFragmentOperation operation)) {
            return false;
        }
        return Objects.equals(
            fragment,
            operation.fragment
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(fragment);
    }
}