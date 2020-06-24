package com.github.smeucci.geo.data.kafka.producer;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.smeucci.geo.data.kafka.config.GeoDataConfig;
import com.github.smeucci.geo.data.kafka.converter.GeoDataConverter;
import com.github.smeucci.geo.data.kafka.record.GeoData;

public class GeoDataProducer {

	private static final Logger log = LoggerFactory.getLogger(GeoDataProducer.class);

	private static final GeoDataConverter converter = new GeoDataConverter();

	public static void main(String[] args) throws Exception {

		// create Producer properties
		Properties properties = new Properties();

		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, GeoDataConfig.Server.KAFKA.address());
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		// create the producer
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

		// create scheduler
		final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleAtFixedRate(() -> produce(producer), 0, 1, TimeUnit.SECONDS);

		// add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			terminate(producer);
		}));

	}

	private static void produce(Producer<String, String> producer) {

		GeoData geoData = GeoData.generate();

		log.info("{}", geoData);

		String json = converter.toJson(geoData);

		// create a producer record
		ProducerRecord<String, String> record = new ProducerRecord<String, String>(
				GeoDataConfig.Topic.SOURCE_GEO_DATA.topicName(), json);

		producer.send(record);

	}

	private static void terminate(Producer<String, String> producer) {
		producer.flush();
		producer.close();
	}

}
