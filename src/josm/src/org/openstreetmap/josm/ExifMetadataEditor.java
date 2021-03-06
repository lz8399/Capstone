package org.openstreetmap.josm;

import java.io.*;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.io.IOUtils;
import org.openstreetmap.josm.data.coor.LatLon;

public class ExifMetadataEditor {

    public void changeExifMetadata(final File jpegImageFile, final File dst, LatLon coords)
            throws IOException, ImageReadException, ImageWriteException {
        OutputStream os = null;
        try {
            TiffOutputSet outputSet = null;

            // note that metadata might be null if no metadata is found.
            //final ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
            //final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) Imaging.getMetadata(jpegImageFile);

            if (null != jpegMetadata) {
                // note that exif might be null if no Exif metadata is found.
                final TiffImageMetadata exif = jpegMetadata.getExif();

                if (null != exif) {
                    // TiffImageMetadata class is immutable (read-only).
                    // TiffOutputSet class represents the Exif data to write.
                    //
                    // Usually, we want to update existing Exif metadata by
                    // changing
                    // the values of a few fields, or adding a field.
                    // In these cases, it is easiest to use getOutputSet() to
                    // start with a "copy" of the fields read from the image.
                    outputSet = exif.getOutputSet();
                }
            }

            // if file does not contain any exif metadata, we create an empty
            // set of exif metadata. Otherwise, we keep all of the other
            // existing tags.
            if (null == outputSet) {
                outputSet = new TiffOutputSet();
            }

            {
                // Example of how to add a field/tag to the output set.
                //
                // Note that you should first remove the field/tag if it already
                // exists in this directory, or you may end up with duplicate
                // tags. See above.
                //
                // Certain fields/tags are expected in certain Exif directories;
                // Others can occur in more than one directory (and often have a
                // different meaning in different directories).
                //
                // TagInfo constants often contain a description of what
                // directories are associated with a given tag.
                //
                final TiffOutputDirectory exifDirectory = outputSet
                        .getOrCreateExifDirectory();
                // make sure to remove old value if present (this method will
                // not fail if the tag does not exist).
                exifDirectory
                        .removeField(ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
                exifDirectory.add(ExifTagConstants.EXIF_TAG_APERTURE_VALUE,
                        new RationalNumber(3, 10));
            }

            {
                // Example of how to add/update GPS info to output set.
                final double longitude = coords.lon();
                final double latitude = coords.lat();

                outputSet.setGPSInDegrees(longitude, latitude);
            }
            final TiffOutputDirectory exifDirectory = outputSet
                    .getOrCreateRootDirectory();
            exifDirectory
                    .removeField(ExifTagConstants.EXIF_TAG_SOFTWARE);
            exifDirectory.add(ExifTagConstants.EXIF_TAG_SOFTWARE,
                    "SomeKind");

            os = new FileOutputStream(dst);
            os = new BufferedOutputStream(os);

            new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os,
                    outputSet);

        } finally {
            IOUtils.closeQuietly(os);
        }
    }
}