package ul.fcul.lasige.find.lib.data;

import android.database.Cursor;

import com.google.common.io.BaseEncoding;

import ul.fcul.lasige.find.lib.data.FindContract.Packets;

/**
 * Created by hugonicolau on 03/11/2015.
 *
 * This class represents a communication packet
 */
public class Packet {
    private final static BaseEncoding HEX_CODER = BaseEncoding.base16();

    // packet id generated by the FIND platform
    private final long mPacketId;
    // received timestamp
    private final long mTimeReceived;
    // source node
    private final byte[] mSourceNode;
    // serialized data
    private final byte[] mData;

    // TODO target node?

    // constructor
    public Packet(long packetId, long timeReceived, byte[] sourceNode, byte[] data){
        mPacketId = packetId;
        mTimeReceived = timeReceived;
        mSourceNode = sourceNode;
        mData = data;
    }

    public long getPacketId() { return mPacketId; }
    public long getTimeReceived() { return mTimeReceived; }
    public byte[] getSourceNode() { return mSourceNode; }

    public String getSourceNodeAsHex() {
        if (mSourceNode != null) {
            return HEX_CODER.encode(mSourceNode);
        }
        return null;
    }
    public byte[] getData() { return mData; }

    /**
     * Return a Packet object from a data cursor. This method is useful to build new packet when
     * querying the FIND service via ContentResolver.
     *
     * @param data Data cursor.
     * @return Packet object.
     */
    public static Packet fromCursor(Cursor data) {
        final int colIdxSourceNode = data.getColumnIndex(Packets.COLUMN_SOURCE_NODE);
        byte[] sourceNode = null;
        if (!data.isNull(colIdxSourceNode)) {
            sourceNode = data.getBlob(colIdxSourceNode);
        }

        return new Packet(
                data.getLong(data.getColumnIndex(Packets._ID)),
                data.getLong(data.getColumnIndex(Packets.COLUMN_TIME_RECEIVED)),
                sourceNode,
                data.getBlob(data.getColumnIndex(Packets.COLUMN_DATA)));
    }
}