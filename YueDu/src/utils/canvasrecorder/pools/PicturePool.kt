package io.YueDu.utils.canvasrecorder.pools

import android.graphics.Picture
import io.YueDu.utils.objectpool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>(64) {

    override fun create(): Picture = Picture()

}
