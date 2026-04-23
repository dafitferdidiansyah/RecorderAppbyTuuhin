package com.eva.feature_player.composable

import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.LocalTimeFormats
import kotlinx.datetime.format

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileMetaDataSheetContent(
	audio: AudioFileModel,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(),
	// TAMBAHAN 1: Parameter untuk menampung teks AI
	aiFullTranscript: String? = null 
) {

	val context = LocalContext.current

	val fileSize = remember(audio.size) {
		Formatter.formatFileSize(context, audio.size)
	}

	val durationText = remember(audio.duration) {
		audio.durationAsLocaltime.format(LocalTimeFormats.LOCALTIME_HH_MM_SS_FORMAT)
	}

	val lastModified = remember(audio.lastModified) {
		audio.lastModified.format(LocalTimeFormats.LOCAL_DATETIME_DATE_TIME_FORMAT)
	}

	LazyColumn(
		modifier = modifier,
		contentPadding = contentPadding,
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		stickyHeader {
			Text(
				text = stringResource(id = R.string.meta_data_sheet_heading),
				style = MaterialTheme.typography.titleLarge,
				color = MaterialTheme.colorScheme.onSurface,
				modifier = Modifier.padding(vertical = 4.dp)
			)
		}

		item {
			FileMetaData(
				title = stringResource(id = R.string.audio_metadata_name_title),
				text = audio.displayName
			)
		}
		item {
			FileMetaData(
				title = stringResource(id = R.string.audio_metadata_file_size_title),
				text = fileSize
			)
		}
		item {
			FileMetaData(
				title = stringResource(id = R.string.audio_metadata_duration_title),
				text = durationText
			)
		}
		item {
			FileMetaData(
				title = stringResource(id = R.string.audio_metadata_mime_type_title),
				text = audio.mimeType
			)
		}
		item {
			FileMetaData(
				title = stringResource(id = R.string.audio_metadata_last_edit_title),
				text = lastModified
			)
		}
		audio.metaData?.let { metaData ->
			item {
				FileMetaData(
					title = stringResource(id = R.string.audio_metadata_channel_title),
					text = stringResource(
						id = if (metaData.channelCount == 1) R.string.audio_metadata_channel_mono
						else R.string.audio_metadata_channel_stereo
					),
				)
			}
			item {
				FileMetaData(
					title = stringResource(id = R.string.audio_metadata_bitrate_title),
					text = stringResource(
						id = R.string.audio_metadata_bitrate,
						metaData.bitRateInKbps
					),
				)
			}
			item {
				FileMetaData(
					title = stringResource(id = R.string.audio_metadata_sample_rate_title),
					text = stringResource(
						id = R.string.audio_metadata_sample_rate,
						metaData.sampleRateInKHz
					),
				)
			}
			metaData.locationString?.let { location ->
				item {
					FileMetaData(
						title = stringResource(R.string.audio_metadata_file_location),
						text = location,
					)
				}
			}
		}
		audio.path?.let { path ->
			item {
				FileMetaData(
					title = stringResource(id = R.string.audio_metadata_path_title),
					text = path,
				)
			}
		}

		// TAMBAHAN 2: Item Transkrip AI di bagian paling bawah
		item {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 16.dp, horizontal = 4.dp)
			) {
				Divider(modifier = Modifier.padding(bottom = 16.dp))
				
				Row(verticalAlignment = Alignment.CenterVertically) {
					Icon(
						imageVector = Icons.Default.Subject,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(20.dp)
					)
					Spacer(modifier = Modifier.width(8.dp))
					Text(
						text = "AI Transcription",
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.primary
					)
				}

				Spacer(modifier = Modifier.padding(vertical = 4.dp))

				if (aiFullTranscript.isNullOrEmpty()) {
					Text(
						text = "No transcript available. Use AI Convert on the main list.",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				} else {
					// Pakai SelectionContainer biar teksnya bisa di-copy/paste
					SelectionContainer {
						Text(
							text = aiFullTranscript,
							style = MaterialTheme.typography.bodyLarge,
							lineHeight = 22.sp,
							color = MaterialTheme.colorScheme.onSurface
						)
					}
				}
				// Padding bawah ekstra biar gak kepentok layar
				Spacer(modifier = Modifier.padding(vertical = 16.dp))
			}
		}
	}
}

@Composable
private fun FileMetaData(
	title: String,
	text: String,
	modifier: Modifier = Modifier,
	titleStyle: TextStyle = MaterialTheme.typography.labelLarge,
	textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
	titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
	textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
	Column(
		modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp)
	) {
		Text(
			text = title,
			style = titleStyle,
			color = titleColor,
		)
		Text(
			text = text,
			style = textStyle,
			color = textColor,
		)
	}
}

@PreviewLightDark
@Composable
private fun FileMetaDataSheetPreview() = RecorderAppTheme {
	Surface(color = MaterialTheme.colorScheme.background) {
		FileMetaDataSheetContent(
			audio = PlayerPreviewFakes.FAKE_AUDIO_MODEL,
			modifier = Modifier.fillMaxWidth(),
			aiFullTranscript = "This is a sample AI transcript to show how it looks in the bottom sheet..."
		)
	}
}