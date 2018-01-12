package com.github.axet.bookreader.widgets;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.axet.bookreader.R;

import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.fbreader.network.BookDownloaderServiceConnection;
import org.geometerplus.android.fbreader.network.NetworkBookInfoActivity;
import org.geometerplus.android.fbreader.network.Util;
import org.geometerplus.android.fbreader.network.action.NetworkBookActions;
import org.geometerplus.android.fbreader.network.action.OpenCatalogAction;
import org.geometerplus.android.fbreader.network.auth.AndroidNetworkContext;
import org.geometerplus.android.fbreader.preferences.EditBookInfoActivity;
import org.geometerplus.android.fbreader.util.AndroidImageSynchronizer;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.book.CoverUtil;
import org.geometerplus.fbreader.book.SeriesInfo;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.network.HtmlUtil;
import org.geometerplus.fbreader.network.NetworkBookItem;
import org.geometerplus.fbreader.network.NetworkCatalogItem;
import org.geometerplus.fbreader.network.NetworkImage;
import org.geometerplus.fbreader.network.NetworkLibrary;
import org.geometerplus.fbreader.network.NetworkTree;
import org.geometerplus.fbreader.network.tree.NetworkBookTree;
import org.geometerplus.fbreader.network.urlInfo.RelatedUrlInfo;
import org.geometerplus.fbreader.network.urlInfo.UrlInfo;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.filesystem.ZLPhysicalFile;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.image.ZLImageProxy;
import org.geometerplus.zlibrary.core.language.Language;
import org.geometerplus.zlibrary.core.language.ZLLanguageUtil;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.util.MimeType;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class BookDialog extends DialogFragment {
    public static final boolean ENABLE_EXTENDED_FILE_INFO = false;

    NetworkBookInfoActivity a;
    public AndroidNetworkContext nc;
    private final BookCollectionShadow myBookCollection = new BookCollectionShadow();
    private final BookDownloaderServiceConnection myConnection = new BookDownloaderServiceConnection();

    View v;
    public NetworkBookTree myTree;
    public NetworkBookItem myBook;
    AndroidImageSynchronizer myImageSynchronizer;
    final ZLResource myResource = ZLResource.resource("bookInfo");

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog d = new AlertDialog.Builder(getActivity())
                .setTitle(myBook.Title)
                .setNeutralButton(getContext().getString(com.github.axet.androidlibrary.R.string.close),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                )
                .setView(createView(LayoutInflater.from(getContext()), null, savedInstanceState))
                .create();
        return d;
    }

    @Nullable
    @Override
    public View getView() {
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return v;
    }

    public View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.network_book, container);

        setupDescription();
        setupExtraLinks();
        setupInfo();
        setupCover();
        //setupButtons();

        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myImageSynchronizer = new AndroidImageSynchronizer(getActivity());
    }

    public View findViewById(int id) {
        return v.findViewById(id);
    }

    private void setTextById(int id, CharSequence text) {
        ((TextView) findViewById(id)).setText(text);
    }

    private void setTextFromResource(int id, String resourceKey) {
        setTextById(id, myResource.getResource(resourceKey).getValue());
    }

    private final void setupDescription() {
        setTextFromResource(org.geometerplus.zlibrary.ui.android.R.id.network_book_description_title, "description");

        CharSequence description = myBook.getSummary();
        if (description == null) {
            description = myResource.getResource("noDescription").getValue();
        }
        final TextView descriptionView = (TextView) findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_description);
        descriptionView.setText(description);
        descriptionView.setMovementMethod(new LinkMovementMethod());
        descriptionView.setTextColor(
                ColorStateList.valueOf(descriptionView.getTextColors().getDefaultColor())
        );
    }

    private final void setupExtraLinks() {
        final List<UrlInfo> extraLinks = myBook.getAllInfos(UrlInfo.Type.Related);
        if (extraLinks.isEmpty()) {
            findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_extra_links_title).setVisibility(View.GONE);
            findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_extra_links).setVisibility(View.GONE);
        } else {
            setTextFromResource(org.geometerplus.zlibrary.ui.android.R.id.network_book_extra_links_title, "extraLinks");
            final LinearLayout extraLinkSection =
                    (LinearLayout) findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_extra_links);
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            View linkView = null;
            for (UrlInfo info : extraLinks) {
                if (!(info instanceof RelatedUrlInfo)) {
                    continue;
                }
                final RelatedUrlInfo relatedInfo = (RelatedUrlInfo) info;
                linkView = inflater.inflate(org.geometerplus.zlibrary.ui.android.R.layout.extra_link_item, extraLinkSection, false);
                linkView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        final NetworkCatalogItem catalogItem =
                                myBook.createRelatedCatalogItem(relatedInfo);
                        if (catalogItem != null) {
                            new OpenCatalogAction(getActivity(), nc)
                                    .run(Util.networkLibrary(getActivity()).getFakeCatalogTree(catalogItem));
                        } else if (MimeType.TEXT_HTML.equals(relatedInfo.Mime)) {
                            Util.openInBrowser(getActivity(), relatedInfo.Url);
                        }
                    }
                });
                ((TextView) linkView.findViewById(org.geometerplus.zlibrary.ui.android.R.id.extra_link_title)).setText(relatedInfo.Title);
                extraLinkSection.addView(linkView);
            }
            linkView.findViewById(org.geometerplus.zlibrary.ui.android.R.id.extra_link_divider).setVisibility(View.GONE);
        }
    }

    private void setPairLabelTextFromResource(int id, String resourceKey) {
        ((TextView) findViewById(id).findViewById(org.geometerplus.zlibrary.ui.android.R.id.book_info_key))
                .setText(myResource.getResource(resourceKey).getValue());
    }

    private void setPairLabelTextFromResource(int id, String resourceKey, int param) {
        ((TextView) findViewById(id).findViewById(org.geometerplus.zlibrary.ui.android.R.id.book_info_key))
                .setText(myResource.getResource(resourceKey).getValue(param));
    }

    private void setPairValueText(int id, CharSequence text) {
        final LinearLayout layout = (LinearLayout) findViewById(id);
        ((TextView) layout.findViewById(org.geometerplus.zlibrary.ui.android.R.id.book_info_value)).setText(text);
    }

    private void setupInfo() {
        setTextFromResource(org.geometerplus.zlibrary.ui.android.R.id.network_book_info_title, "bookInfo");

        setPairLabelTextFromResource(org.geometerplus.zlibrary.ui.android.R.id.network_book_title, "title");
        setPairLabelTextFromResource(org.geometerplus.zlibrary.ui.android.R.id.network_book_authors, "authors");
        setPairLabelTextFromResource(org.geometerplus.zlibrary.ui.android.R.id.network_book_series_title, "series");
        setPairLabelTextFromResource(org.geometerplus.zlibrary.ui.android.R.id.network_book_series_index, "indexInSeries");
        setPairLabelTextFromResource(org.geometerplus.zlibrary.ui.android.R.id.network_book_catalog, "catalog");

        setPairValueText(org.geometerplus.zlibrary.ui.android.R.id.network_book_title, myBook.Title);

        if (myBook.Authors.size() > 0) {
            findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_authors).setVisibility(View.VISIBLE);
            final StringBuilder authorsText = new StringBuilder();
            for (NetworkBookItem.AuthorData author : myBook.Authors) {
                if (authorsText.length() > 0) {
                    authorsText.append(", ");
                }
                authorsText.append(author.DisplayName);
            }
            setPairLabelTextFromResource(org.geometerplus.zlibrary.ui.android.R.id.network_book_authors, "authors", myBook.Authors.size());
            setPairValueText(org.geometerplus.zlibrary.ui.android.R.id.network_book_authors, authorsText);
        } else {
            findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_authors).setVisibility(View.GONE);
        }

        if (myBook.SeriesTitle != null) {
            findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_series_title).setVisibility(View.VISIBLE);
            setPairValueText(org.geometerplus.zlibrary.ui.android.R.id.network_book_series_title, myBook.SeriesTitle);
            final float indexInSeries = myBook.IndexInSeries;
            if (indexInSeries > 0) {
                final String seriesIndexString;
                if (Math.abs(indexInSeries - Math.round(indexInSeries)) < 0.01) {
                    seriesIndexString = String.valueOf(Math.round(indexInSeries));
                } else {
                    seriesIndexString = String.format("%.1f", indexInSeries);
                }
                setPairValueText(org.geometerplus.zlibrary.ui.android.R.id.network_book_series_index, seriesIndexString);
                findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_series_index).setVisibility(View.VISIBLE);
            } else {
                findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_series_index).setVisibility(View.GONE);
            }
        } else {
            findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_series_title).setVisibility(View.GONE);
            findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_series_index).setVisibility(View.GONE);
        }

        if (myBook.Tags.size() > 0) {
            findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_tags).setVisibility(View.VISIBLE);
            final StringBuilder tagsText = new StringBuilder();
            for (String tag : myBook.Tags) {
                if (tagsText.length() > 0) {
                    tagsText.append(", ");
                }
                tagsText.append(tag);
            }
            setPairLabelTextFromResource(org.geometerplus.zlibrary.ui.android.R.id.network_book_tags, "tags", myBook.Tags.size());
            setPairValueText(org.geometerplus.zlibrary.ui.android.R.id.network_book_tags, tagsText);
        } else {
            findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_tags).setVisibility(View.GONE);
        }

        setPairValueText(org.geometerplus.zlibrary.ui.android.R.id.network_book_catalog, myBook.Link.getTitle());
    }

    private final void setupCover() {
        final View rootView = findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_root);
        final ImageView coverView = (ImageView) findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_cover);

        final DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final int maxHeight = metrics.heightPixels * 2 / 3;
        final int maxWidth = maxHeight * 2 / 3;
        Bitmap coverBitmap = null;
        final ZLImage cover = NetworkTree.createCoverForItem(Util.networkLibrary(getContext()), myBook, false);
        if (cover != null) {
            ZLAndroidImageData data = null;
            final ZLAndroidImageManager mgr = (ZLAndroidImageManager) ZLAndroidImageManager.Instance();
            if (cover instanceof ZLImageProxy) {
                final ZLImageProxy img = (ZLImageProxy) cover;
                img.startSynchronization(myImageSynchronizer, new Runnable() {
                    public void run() {
                        if (img instanceof NetworkImage) {
                            ((NetworkImage) img).synchronizeFast();
                        }
                        final ZLAndroidImageData data = mgr.getImageData(img);
                        if (data != null) {
                            final Bitmap coverBitmap = data.getBitmap(maxWidth, maxHeight);
                            if (coverBitmap != null) {
                                coverView.setImageBitmap(coverBitmap);
                                coverView.setVisibility(View.VISIBLE);
                                rootView.invalidate();
                                rootView.requestLayout();
                            }
                        }
                    }
                });
            } else {
                data = mgr.getImageData(cover);
            }
            if (data != null) {
                coverBitmap = data.getBitmap(maxWidth, maxHeight);
            }
        }
        if (coverBitmap != null) {
            coverView.setImageBitmap(coverBitmap);
            coverView.setVisibility(View.VISIBLE);
        } else {
            coverView.setVisibility(View.GONE);
        }
    }

    private final void setupButtons() {
        final int buttons[] = new int[]{
                org.geometerplus.zlibrary.ui.android.R.id.network_book_button0,
                org.geometerplus.zlibrary.ui.android.R.id.network_book_button1,
                org.geometerplus.zlibrary.ui.android.R.id.network_book_button2,
                org.geometerplus.zlibrary.ui.android.R.id.network_book_button3,
        };
        final List<NetworkBookActions.NBAction> actions = NetworkBookActions.getContextMenuActions(getActivity(), myTree, myBookCollection, myConnection);

        final boolean skipSecondButton =
                actions.size() < buttons.length &&
                        actions.size() % 2 == 1;
        int buttonNumber = 0;
        for (final NetworkBookActions.NBAction a : actions) {
            if (skipSecondButton && buttonNumber == 1) {
                ++buttonNumber;
            }
            if (buttonNumber >= buttons.length) {
                break;
            }

            final int buttonId = buttons[buttonNumber++];
            TextView button = (TextView) findViewById(buttonId);
            button.setText(a.getContextLabel(null));
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    a.run(myTree);
                    BookDialog.this.updateView();
                }
            });
            button.setEnabled(a.isEnabled(null));
        }
        findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_left_spacer).setVisibility(skipSecondButton ? View.VISIBLE : View.GONE);
        findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_right_spacer).setVisibility(skipSecondButton ? View.VISIBLE : View.GONE);
        if (skipSecondButton) {
            final int buttonId = buttons[1];
            View button = findViewById(buttonId);
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
        }
        while (buttonNumber < buttons.length) {
            final int buttonId = buttons[buttonNumber++];
            View button = findViewById(buttonId);
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
        }
    }

    private void updateView() {
        setupButtons();
        final View rootView = findViewById(org.geometerplus.zlibrary.ui.android.R.id.network_book_root);
        rootView.invalidate();
        rootView.requestLayout();
    }
}
